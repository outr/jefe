package com.outr.jefe.boot

import java.nio.file.{Files, Paths}

import com.outr.jefe.{BuildInfo, Jefe}
import com.outr.jefe.boot.command._
import com.outr.jefe.resolve.{MavenRepository, Repositories}
import com.outr.jefe.server.JefeServer
import io.youi.stream._
import profig.{FileType, Profig, ProfigLookupPath, ProfigPath}

object JefeBoot {
  private lazy val userHome = Paths.get(System.getProperty("user.home"))
  private lazy val root = userHome.resolve(".jefe")
  lazy val config: ProfigPath = Profig("jefe")

  private lazy val configPath = root.resolve("config.json")

  lazy val additionalRepositories: List[MavenRepository] = config("repositories").opt[List[MavenRepository]].getOrElse(Nil)
  lazy val repositories: Repositories = Repositories.default.withRepositories(additionalRepositories: _*)

  val commands: List[Command] = List(
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

    // Load config files in root directory
    Profig.load(ProfigLookupPath.defaults.map { p =>
      p.copy(path = root.resolve(p.path).toFile.getAbsolutePath)
    }: _*)
    Profig.loadDefaults()
    Profig.merge(args)
    Jefe.baseDirectory = root
    JefeServer.initLogging()

    scribe.info(s"Jefe version ${BuildInfo.version}")
    scribe.info("------------------------")
    Profig("arg1").opt[String] match {
      case Some(commandName) => commandsMap.get(commandName) match {
        case Some(command) => command.execute()
        case None => {
          scribe.info(s"Unknown command: $commandName")
          scribe.info("")
          help()
        }
      }
      case None => {
        scribe.info("jefe: missing command")
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
    scribe.info("Usage: jefe [command] [options]")
    scribe.info("")
    scribe.info("Parameters:")
    scribe.info("\t--authBind=true: to enable authbind on this run of jefe")
    scribe.info("")
    scribe.info("Valid Commands:")
    commands.foreach { command =>
      scribe.info(s"  ${command.name}: ${command.description}")
    }
  }
}