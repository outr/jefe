package com.outr.jefe.server

import com.outr.scribe.Logging
import org.powerscala.command.{Command, CommandImplementation, CommandInterpreter, CommandManager, StandardIO}

object CommandSupport extends CommandManager with CommandInterpreter with Logging {
  override def implementation: CommandImplementation = new StandardIO {
    override def send(line: String): Unit = {
      System.out.println(line)
    }
  }
  override def interpreter: CommandInterpreter = this

  def init(): Unit = {
  }

  override def process(command: Command): Unit = command match {
    case Help => println(
      """Commands:
        | help: this command
        | list: list all the application configurations currently loaded
        | update: update all directories checking for changes
        | quit: shuts down all applications and terminates jefe
      """.stripMargin)
    case ListApps => JefeServer.list()
    case Update => JefeServer.updateDirectories()
    case Quit => JefeServer.shutdown()
  }

  override def toCommand(line: String): Option[Command] = line.toLowerCase match {
    case "help" | "?" => Some(Help)
    case "list" => Some(ListApps)
    case "update" => Some(Update)
    case "quit" | "stop" | "exit" => Some(Quit)
    case _ => None
  }

  override def fromCommand(command: Command): String = command match {
    case _ => throw new UnsupportedOperationException(s"Command cannot be sent: $command.")
  }
}

object Help extends Command

object ListApps extends Command

object Update extends Command

object Quit extends Command