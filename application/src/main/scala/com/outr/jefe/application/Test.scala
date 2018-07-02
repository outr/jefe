package com.outr.jefe.application

import com.outr.jefe.resolve._

object Test {
  def main(args: Array[String]): Unit = {
    val app = ProcessApplication.artifact(
      artifacts = List("io.youi" %% "youi-example" % "latest.release"),
      mainClass = Some("io.youi.example.ServerExampleApplication")
    )
    app.start()
    scribe.info("Started!")
    Thread.sleep(30000)
    scribe.info("Dying!")
    app.stop(force = false)
  }
}