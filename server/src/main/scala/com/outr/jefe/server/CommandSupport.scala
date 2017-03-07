package com.outr.jefe.server

import scribe.{Logging, Platform}
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

  override def process(command: Command): Unit = try {
    command match {
      case Help => println(
        """Commands:
          | help: this command
          | list: list all the application configurations currently loaded
          | status: show status information for all applications
          | update: update all directories checking for changes
          | quit: shuts down all applications and terminates jefe
          | enable: enables an application and then reloads the configuration
          | disable: disables an application and then reloads the configuration
        """.stripMargin)
      case ListApps => println(JefeServer.list())
      case Status => println(JefeServer.status())
      case Update => JefeServer.updateDirectories()
      case Enable(appName) => JefeServer.changeEnabled(appName, enable = true)
      case Disable(appName) => JefeServer.changeEnabled(appName, enable = false)
      case Quit => JefeServer.shutdown()
    }
  } catch {
    case t: Throwable => logger.error(t)
  }

  override def toCommand(line: String): Option[Command] = if (line != null) {
    line.toLowerCase match {
      case "help" | "?" => Some(Help)
      case "list" => Some(ListApps)
      case "status" => Some(Status)
      case "update" => Some(Update)
      case l if l.startsWith("enable ") => Some(Enable(l.substring(7)))
      case l if l.startsWith("disable ") => Some(Disable(l.substring(8)))
      case "quit" | "stop" | "exit" => Some(Quit)
      case _ => None
    }
  } else {
    None
  }

  override def fromCommand(command: Command): String = command match {
    case _ => throw new UnsupportedOperationException(s"Command cannot be sent: $command.")
  }
}

object Status extends Command

object Help extends Command

object ListApps extends Command

object Update extends Command

case class Enable(appName: String) extends Command

case class Disable(appName: String) extends Command

object Quit extends Command