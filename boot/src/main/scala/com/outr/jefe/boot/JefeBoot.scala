package com.outr.jefe.boot

import java.nio.file.{Files, Paths}

import com.outr.jefe.Jefe
import com.outr.jefe.boot.command._
import com.outr.jefe.resolve.{MavenRepository, Repositories}
import org.powerscala.io.IO
import profig.{FileType, Profig}
import scribe.{Level, Logger}
import scribe.format._
import scribe.writer.FileWriter
import scribe.writer.file.LogPath

object JefeBoot {
  private lazy val userHome = Paths.get(System.getProperty("user.home"))
  private lazy val root = userHome.resolve(".jefe")
  lazy val config = Profig("jefe")

  private lazy val configPath = root.resolve("config.json")

  lazy val additionalRepositories: List[MavenRepository] = config("repositories").opt[List[MavenRepository]].getOrElse(Nil)
  lazy val repositories: Repositories = Repositories.default.withRepositories(additionalRepositories: _*)

  val commands = List(
    RunCommand,
    SetCommand,
    GetCommand,
    RepositoryCommand,
    ServerCommand,
    StartCommand,
    StopCommand,
    ServiceCommand,
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
    Jefe.baseDirectory = root

    Logger
      .root
      .clearHandlers()
      .withHandler(
        minimumLevel = Some(Level.Info),
        writer = FileWriter().path(LogPath.daily("jefe", directory = Jefe.baseDirectory.resolve("logs")))
      )

    Profig("arg1").opt[String] match {
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