package com.outr.jefe.server

import com.outr.jefe.application.ApplicationManager
import com.outr.jefe.server.service._
import io.youi.http.Method
import io.youi.server.Server
import profig.Profig
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
          ),
          "stop" / StopServer
        )
      )
    )

    // TODO: Load configuration
  }

  def applications: ApplicationManager.type = ApplicationManager

  override def dispose(): Unit = {
    scribe.warn("Shutting down")

    applications.dispose()

    super.dispose()
  }
}