package com.outr.jefe.boot.command

import com.outr.jefe.application.Application

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scribe.Execution.global

object StartCommand extends ApplicationCommand {
  override def name: String = "start"
  override def description: String = "Starts an application on the server"

  override protected def allowBackground: Boolean = false

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
    val response = Await.result(future, 15.seconds)
    assert(response.success, s"Response was not successful! Errors: ${response.errors}")
    logger.info(s"${application.id} started successfully")
    sys.exit(0)
  }
}
