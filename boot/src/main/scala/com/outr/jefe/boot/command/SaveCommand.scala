package com.outr.jefe.boot.command

import scala.concurrent.Await
import scala.concurrent.duration._

object SaveCommand extends Command {
  override def name: String = "save"
  override def description: String = "Saves the applications list to the server"

  override def execute(): Unit = {
    val future = ServerCommand.client.application.save()
    try {
      val response = Await.result(future, 15.seconds)
      assert(response.success, s"Response was not successful! Errors: ${response.errors}")
      logger.info("Applications list saved successfully")
    } finally {
      sys.exit(0)
    }
  }

  override def help(): Unit = {
    logger.info(description)
  }
}