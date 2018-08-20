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

  val token: String = Profig("jefe.token").opt[String].getOrElse {
    val generated = Unique(length = 8, characters = Unique.Readable)
    scribe.warn(s"No jefe.token specified in configuration, so a runtime value was generated: $generated")
    generated
  }

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
        )
      )
    )
  )

  def applications: ApplicationManager.type = ApplicationManager

  override def dispose(): Unit = {
    applications.dispose()

    super.dispose()
  }
}