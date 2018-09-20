package com.outr.jefe.boot.command

import profig.Profig

import scala.concurrent.Await
import scala.concurrent.duration._

object StopCommand extends Command {
  override def name: String = "stop"
  override def description: String = "Stops an application on the server"

  override def execute(): Unit = {
    Profig("arg2").opt[String] match {
      case Some(applicationId) => {
        val future = ServerCommand.client.application.stop(applicationId)
        val response = Await.result(future, 15.seconds)
        assert(response.success, s"Response was not successful! Errors: ${response.errors}")
        logger.info(s"$applicationId stopped successfully")
        sys.exit(0)
      }
      case None => {
        logger.info("The application id to be stopped must be included!")
        logger.info("")
        help()
      }
    }
  }

  override def help(): Unit = logger.info("Usage: jefe stop [applicationId]")
}