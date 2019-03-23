package com.outr.jefe.server

import java.nio.file.{Files, Paths}

import com.outr.jefe.Jefe
import com.outr.jefe.application.{Application, ApplicationManager}
import com.outr.jefe.server.service._
import io.circe.Printer
import io.youi.http.Method
import io.youi.server.Server
import profig.{JsonUtil, Profig}
import io.youi.server.dsl._
import io.circe.generic.auto._
import io.youi.Unique

import scala.concurrent.Future
import scribe.Execution.global

object JefeServer extends Server {
  Jefe.baseDirectory = Paths.get(System.getProperty("user.home")).resolve(".jefe")
  Profig.defaults(List("--listeners.http.port", "10565"))

  val host: String = Profig("listeners.http.host").as[String]("127.0.0.1")
  val port: Int = Profig("listeners.http.port").as[Int]
  val token: String = Profig("jefe.token").opt[String].getOrElse {
    val generated = Unique(length = 8, characters = Unique.Readable)
    scribe.warn(s"No jefe.token specified in configuration, so a runtime value was generated: $generated")
    generated
  }

  lazy val persistence: Boolean = Profig("jefe.server.persistence").as[Boolean](true)
  private lazy val applicationsPath = Jefe.baseDirectory.resolve("applications.json")
  private lazy val proxiesPath = Jefe.baseDirectory.resolve("proxies.json")

  override protected def init(): Future[Unit] = super.init().map { _ =>
    if (!persistence) scribe.warn("Server persistence is disabled")

    handler(
      filters(
        SecurityFilter / Method.Post / List(
          "application" / List(
            "create" / CreateApplication,
            "start" / StartApplication,
            "stats" / StatsApplication,
            "stop" / StopApplication,
            "restart" / RestartApplication,
            "remove" / RemoveApplication
            // TODO: EnableApplication / DisableApplication
          ),
          "proxy" / List(
            "add" / AddProxy,
            "remove" / RemoveProxy
          ),
          "stop" / StopServer
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

  override def dispose(): Unit = {
    scribe.warn("Shutting down")

    applications.dispose()

    super.dispose()
  }
}