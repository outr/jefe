package com.outr.jefe.boot.command

import com.outr.jefe.application.Application
import profig.Profig

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scribe.Execution.global

object StartCommand extends ApplicationCommand {
  override def name: String = "start"
  override def description: String = "Starts an application on the server"

  override protected def allowBackground: Boolean = false

  override def execute(): Unit = loadApplication() match {
    case Some(application) => execute(application)
    case None => {
      val applicationId = Profig("arg2").as[String]
      try {
        val future = ServerCommand.client.application.start(applicationId)
        val response = Await.result(future, 5.minutes)
        assert(response.success, s"Response was not successful! Errors: ${response.errors}")
        logger.info(s"$applicationId started successfully")
      } finally {
        sys.exit(0)
      }
    }
  }

  override def execute(application: Application): Unit = {
    val future = ServerCommand.client.application.create(application).flatMap { response =>
      if (response.success) {
        if (application.enabled) {
          ServerCommand.client.application.start(application.id)
        } else {
          Future.successful(response)
        }
      } else {
        Future.successful(response)
      }
    }
    val response = Await.result(future, 5.minutes)
    assert(response.success, s"Response was not successful! Errors: ${response.errors}")
    logger.info(s"${application.id} started successfully")
    sys.exit(0)
  }
}
