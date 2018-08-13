package com.outr.jefe.boot

import java.nio.file.{Files, Paths}

import com.outr.jefe.boot.command._
import org.powerscala.io.IO
import profig.{FileType, Profig}
import scribe.Logger
import scribe.format._

object JefeBoot {
  private lazy val userHome = Paths.get(System.getProperty("user.home"))
  private lazy val root = userHome.resolve(".jefe")
  lazy val config = Profig("jefe")

  private lazy val configPath = root.resolve("config.json")

  val commands = List(
    RunCommand,
    SetCommand,
    GetCommand,
    ServeCommand,
    HelpCommand
  )
  lazy val commandsMap: Map[String, Command] = commands.map(c => c.name -> c).toMap

  lazy val logger: Logger = Logger.empty.orphan().withHandler(formatter = Formatter.simple)

  def main(args: Array[String]): Unit = {
    if (Files.exists(configPath)) {
      val file = configPath.toFile
      config.merge(file, FileType.Json)
    }

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

  def save(): Unit = {
    val json = config()
    val jsonString = json.pretty(io.circe.Printer.spaces2)
    Files.createDirectories(root)
    IO.stream(jsonString, configPath.toFile)
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