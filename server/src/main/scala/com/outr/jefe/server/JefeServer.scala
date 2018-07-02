package com.outr.jefe.server

import com.outr.jefe.resolve._
import com.outr.jefe.application.{Application, ProcessApplication}
import io.youi.server.Server
import profig.Profig
import reactify.{Val, Var}
import reactify.instance.RecursionMode

object JefeServer extends Server {
  Profig.defaults(List("--listeners.http.port", "10565"))

  object applications {
    private val _applications: Var[List[Application]] = Var(Nil, recursion = RecursionMode.None)

    def all: Val[List[Application]] = _applications

    def +=(application: Application): Application = synchronized {
      this -= application
      _applications := application :: _applications()
      application
    }

    def -=(application: Application): Application = synchronized {
      application.stop(force = false)
      _applications := _applications().filterNot(_.id == application.id)
      application
    }

    def launch(application: Application): Application = {
      this += application
      application.start()
      application
    }
  }

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