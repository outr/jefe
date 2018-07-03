package com.outr.jefe.server

import com.outr.jefe.resolve._
import com.outr.jefe.application.{ApplicationManager, ProcessApplication}
import com.outr.jefe.server.service.{CreateApplication, RemoveApplication, StartApplication, StopApplication}
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
          "stop" / StopApplication,
          "remove" / RemoveApplication
        )
      )
    )
  )

  def applications: ApplicationManager.type = ApplicationManager

  override def dispose(): Unit = {
    applications.all().foreach { app =>
      applications -= app
    }

    super.dispose()
  }

  def main(args: Array[String]): Unit = {
    Profig.loadDefaults()
    Profig.merge(args)

    start()

    scribe.info("Waiting ten seconds...")
    Thread.sleep(10000)
    scribe.info("Creating server...")
    val app = ProcessApplication.artifact(
      id = "youi-exmaple",
      artifacts = List("io.youi" %% "youi-example" % "latest.release"),
      mainClass = Some("io.youi.example.ServerExampleApplication")
    )
    applications.launch(app)
    scribe.info("Created! Waiting fifteen seconds...")
    Thread.sleep(15000)
    dispose()
  }
}