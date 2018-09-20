package com.outr.jefe.server

import java.nio.file.Files

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

object JefeServer extends Server {
  Profig.defaults(List("--listeners.http.port", "10565"))

  val host: String = Profig("listeners.http.host").as[String]("127.0.0.1")
  val port: Int = Profig("listeners.http.port").as[Int]
  val token: String = Profig("jefe.token").opt[String].getOrElse {
    val generated = Unique(length = 8, characters = Unique.Readable)
    scribe.warn(s"No jefe.token specified in configuration, so a runtime value was generated: $generated")
    generated
  }

  private lazy val applicationsPath = Jefe.baseDirectory.resolve("applications.json")

  override protected def init(): Unit = {
    super.init()

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
          "stop" / StopServer
        )
      )
    )

    // Load configuration
    if (Files.exists(applicationsPath)) {
      synchronized {
        val jsonString = new String(Files.readAllBytes(applicationsPath), "UTF-8")
        val applications = JsonUtil.fromJsonString[List[Application]](jsonString)
        applications.foreach { application =>
          JefeServer.applications += application
          if (application.enabled) {
            application.start()
          }
        }
      }
    }
  }

  def save(): Unit = synchronized {
    val jsonString = JsonUtil.toJson(applications.all()).pretty(Printer.spaces2)
    Files.write(applicationsPath, jsonString.getBytes("UTF-8"))
  }

  def applications: ApplicationManager.type = ApplicationManager

  override def dispose(): Unit = {
    scribe.warn("Shutting down")

    applications.dispose()

    super.dispose()
  }
}