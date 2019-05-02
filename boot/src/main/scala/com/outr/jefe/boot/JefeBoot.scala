package com.outr.jefe.boot

import java.nio.file.{Files, Paths}

import com.outr.jefe.{BuildInfo, Jefe}
import com.outr.jefe.boot.command._
import com.outr.jefe.launch.Launcher
import com.outr.jefe.resolve.{MavenRepository, Repositories}
import com.outr.jefe.server.JefeServer
import org.powerscala.io.IO
import profig.{FileType, Profig, ProfigPath}
import scribe.{Level, Logger}
import scribe.writer.FileWriter
import scribe.writer.file.LogPath

object JefeBoot {
  private lazy val userHome = Paths.get(System.getProperty("user.home"))
  private lazy val root = userHome.resolve(".jefe")
  lazy val config: ProfigPath = Profig("jefe")

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
    ListCommand,
    StopCommand,
    SaveCommand,
    ProxyCommand,
    ServiceCommand,
    HttpCommand,
    HelpCommand
  )
  lazy val commandsMap: Map[String, Command] = commands.map(c => c.name -> c).toMap

  def main(args: Array[String]): Unit = {
    if (Files.exists(configPath)) {
      val file = configPath.toFile
      config.merge(file, FileType.Json)
    }

    Profig.loadDefaults()
    Profig.merge(args)
    Jefe.baseDirectory = root
    JefeServer.initLogging()

    Logger
      .root
      .clearHandlers()
      .withHandler(
        minimumLevel = Some(Level.Info),
        writer = FileWriter().path(LogPath.daily("jefe", directory = Jefe.baseDirectory.resolve("logs")))
      )

    scribe.info(s"Jefe version ${BuildInfo.version}")
    scribe.info("------------------------")
    Profig("arg1").opt[String] match {
      case Some(commandName) => commandsMap.get(commandName) match {
        case Some(command) => command.execute()
        case None => {
          Launcher.logger.info(s"Unknown command: $commandName")
          Launcher.logger.info("")
          help()
        }
      }
      case None => {
        Launcher.logger.info("jefe: missing command")
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

  def version(): Unit = {

  }

  def help(): Unit = {
    Launcher.logger.info("Usage: jefe [command] [options]")
    Launcher.logger.info("")
    Launcher.logger.info("Valid Commands:")
    commands.foreach { command =>
      Launcher.logger.info(s"  ${command.name}: ${command.description}")
    }
  }
}