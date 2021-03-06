package com.outr.jefe.server

import java.nio.file.{Files, Paths => NIOPaths}

import com.outr.jefe.{Jefe, Paths}
import com.outr.jefe.application.{Application, ApplicationManager}
import com.outr.jefe.server.service._
import io.circe.Printer
import io.youi.server.Server
import profig.{JsonUtil, Profig}
import io.youi.server.dsl._
import io.circe.generic.auto._
import io.youi.Unique

import scala.concurrent.Future
import scribe.Execution.global
import scribe.{Level, LogRecord, Logger, MDC}
import scribe.format.{Formatter, cyan, message, string}
import scribe.output.{CompositeOutput, LogOutput}
import scribe.writer.FileWriter
import scribe.writer.file.LogPath

object JefeServer extends Server {
  private val formatter = new Formatter {
    override def format[M](record: LogRecord[M]): LogOutput = MDC.get("application") match {
      case Some(application) => new CompositeOutput(List(
        string("["),
        cyan(string(application)),
        string("] "),
        message
      ).map(_.format(record)))
      case None => Formatter.simple.format(record)
    }
  }

  Jefe.baseDirectory = NIOPaths.get(System.getProperty("user.home")).resolve(".jefe")
  Profig.defaults(List("--listeners.http.port", "10565"))

  val host: String = Profig("listeners.http.host").as[String]("127.0.0.1")
  val port: Int = Profig("listeners.http.port").as[Int]
  lazy val token: String = Profig("jefe.token").opt[String].getOrElse {
    val generated = Unique(length = 16)
    scribe.warn(s"No jefe.token specified in configuration, so a runtime value was generated: $generated")
    generated
  }

  lazy val persistence: Boolean = Profig("jefe.server.persistence").as[Boolean](true)
  private lazy val applicationsPath = Jefe.baseDirectory.resolve("applications.json")
  private lazy val proxiesPath = Jefe.baseDirectory.resolve("proxies.json")

  override protected def init(): Future[Unit] = super.init().map { _ =>
    initLogging()

    if (!persistence) scribe.warn("Server persistence is disabled")

    handler(
      filters(
        SecurityFilter / List(
          // Application
          Paths.application.create / CreateApplication,
          Paths.application.start / StartApplication,
          Paths.application.stats / StatsApplication,
          Paths.application.list / ListApplications,
          Paths.application.stop / StopApplication,
          Paths.application.save / SaveApplications,
          Paths.application.restart / RestartApplication,
          Paths.application.remove / RemoveApplication,
          // TODO: EnableApplication / DisableApplication
          // Proxy
          Paths.proxy.add / AddProxy,
          Paths.proxy.remove / RemoveProxy,
          // Server
          Paths.version / VersionServer,
          Paths.stop / StopServer
        )
      )
    )

    // Load configuration
    if (persistence) {
      synchronized {
        // Load applications
        if (Files.exists(applicationsPath)) {
          val applicationsJson = new String(Files.readAllBytes(applicationsPath), "UTF-8")
          val applications = JsonUtil.fromJsonString[List[Application]](applicationsJson)
          applications.foreach { application =>
            JefeServer.applications += application
            if (application.enabled) {
              application.start()
            }
          }
        }

        // Load proxies
        if (Files.exists(proxiesPath)) {
          val proxiesJson = new String(Files.readAllBytes(proxiesPath), "UTF-8")
          JsonUtil.fromJsonString[List[ProxyConfig]](proxiesJson).foreach { proxy =>
            proxies += proxy
          }
        }
      }
    }
  }

  def save(): Unit = synchronized {
    if (persistence) {
      Files.write(
        applicationsPath,
        JsonUtil.toJson(applications.all()).pretty(Printer.spaces2).getBytes("UTF-8")
      )
      Files.write(
        proxiesPath,
        JsonUtil.toJson(proxies.items().collect {
          case pc: ProxyConfig => pc
        }).pretty(Printer.spaces2).getBytes("UTF-8")
      )
    }
  }

  def applications: ApplicationManager.type = ApplicationManager

  def main(args: Array[String]): Unit = start()

  def initLogging(): Unit = Logger
    .root
    .clearHandlers()
    .withHandler(formatter, minimumLevel = Some(Level.Info))
    .withHandler(
      formatter = formatter,
      minimumLevel = Some(Level.Info),
      writer = FileWriter().path(LogPath.daily("jefe", directory = Jefe.baseDirectory.toAbsolutePath.resolve("logs")))
    )
    .replace()

  override def dispose(): Unit = {
    scribe.warn("Shutting down")

    applications.dispose()

    super.dispose()
  }
}