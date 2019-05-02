package com.outr.jefe.boot.command

import com.outr.jefe.boot.JefeBoot
import profig.Profig

object GetCommand extends Command {
  override def name: String = "get"
  override def description: String = "Gets a persistent variable in Jefe configuration (~/.jefe/config.json)"

  override def execute(): Unit = Profig("arg2").opt[String] match {
    case Some(key) => {
      JefeBoot.config(key).opt[String] match {
        case Some(value) => logger.info(s"$key=$value")
        case None => logger.info(s"No value found for $key")
      }
    }
    case None => {
      val jsonString = JefeBoot.config().pretty(io.circe.Printer.spaces2)
      logger.info("Stored configuration:")
      logger.info(jsonString)
    }
  }

  override def help(): Unit = {
    logger.info("Usage: jefe get (key)")
    logger.info("")
    logger.info("Retrieves the value for the key if specified. Otherwise, outputs all stored configuration.")
  }
}