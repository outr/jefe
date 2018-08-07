package com.outr.jefe.boot.command

import com.outr.jefe.boot.JefeBoot
import profig.Profig

object GetCommand extends Command {
  override def name: String = "get"
  override def description: String = "Gets a persistent variable in Jefe configuration (~/.jefe/config.json)"

  override def execute(): Unit = Profig("arg2").as[Option[String]] match {
    case Some(key) => {
      JefeBoot.config(key).as[Option[String]] match {
        case Some(value) => logger.info(s"$key=$value")
        case None => logger.info(s"No value found for $key")
      }
    }
    case None => {
      logger.info("Exactly one argument must be included!")
      help()
    }
  }

  override def help(): Unit = {
    logger.info("Usage: jefe get [key]")
  }
}