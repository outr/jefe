package com.outr.jefe.server

import com.outr.jefe.resolve._
import com.outr.jefe.application.{ApplicationManager, ProcessApplication}
import com.outr.jefe.server.service._
import io.youi.http.Method
import io.youi.server.Server
import profig.Profig
import io.youi.server.dsl._
import io.circe.generic.auto._

object JefeServer extends Server {
  Profig.defaults(List("--listeners.http.port", "10565"))

  handler(
    filters(
      Method.Post / List(
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