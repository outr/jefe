package com.outr.jefe.boot.command

import scala.concurrent.Await
import scala.concurrent.duration._

object ListCommand extends Command {
  override def name: String = "list"
  override def description: String = "Lists all applications and their status on the server"

  override def execute(): Unit = {
    val future = ServerCommand.client.application.list()
    try {
      val response = Await.result(future, 15.seconds)
      assert(response.success, s"Response was not successful! Errors: ${response.errors}")
      logger.info(s"Server stats:")
      val stats = response.stats
      stats.toList.map(s => s"\t$s").foreach { s =>
        logger.info(s)
      }
      logger.info(s"Applications (${response.applicationStats.size}):")
      response.applicationStats.foreach { stat =>
        logger.info(stat.toString)
      }
    } finally {
      sys.exit(0)
    }
  }

  override def help(): Unit = {
    logger.info(description)
  }
}