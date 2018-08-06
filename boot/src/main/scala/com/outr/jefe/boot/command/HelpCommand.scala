package com.outr.jefe.boot.command

import com.outr.jefe.boot.JefeBoot
import profig.Profig

object HelpCommand extends Command {
  override def name: String = "help"
  override def description: String = "Additional information about using Jefe"

  override def execute(): Unit = Profig("arg2").as[Option[String]] match {
    case Some(command) => JefeBoot
      .commandsMap
      .getOrElse(command, throw new RuntimeException(s"$command not found"))
      .help()
    case None => help()
  }

  override def help(): Unit = {
    logger.info("General help or help for a specific command")
    logger.info("")
    logger.info("Usage: jefe help [command]")
  }
}
