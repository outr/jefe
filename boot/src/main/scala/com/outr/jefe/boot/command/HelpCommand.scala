package com.outr.jefe.boot.command

class HelpCommand extends Command {
  override def name: String = "help"
  override def description: String = "Additional information about using Jefe"

  override def execute(): Unit = ???

  override def help(): Unit = ???
}
