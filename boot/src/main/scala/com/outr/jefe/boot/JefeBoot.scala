package com.outr.jefe.boot

import com.outr.jefe.boot.command.{Command, HelpCommand}
import profig.Profig
import scribe.Logger
import scribe.format._

object JefeBoot {
  private val commands = List(
    new HelpCommand
  )
  private lazy val commandsMap: Map[String, Command] = commands.map(c => c.name -> c).toMap

  lazy val logger: Logger = Logger.empty.orphan().withHandler(formatter = Formatter.simple)

  def main(args: Array[String]): Unit = {
    Profig.loadDefaults()
    Profig.merge(args)

    Profig("arg1").as[Option[String]] match {
      case Some(commandName) => commandsMap.get(commandName) match {
        case Some(command) => command.execute()
        case None => {
          logger.info(s"Unknown command: $commandName")
          logger.info("")
          help()
        }
      }
      case None => {
        logger.info("jefe: missing command")
        help()
      }
    }
  }

  def help(): Unit = {
    logger.info("Usage: jefe [command] [options]")
    logger.info("")
    logger.info("Valid Commands:")
    commands.foreach { command =>
      logger.info(s"  ${command.name}: ${command.description}")
    }
  }
}
