package com.outr.jefe.boot.command

import com.outr.jefe.launch.Launcher
import scribe.Logger

trait Command {
  def name: String
  def description: String
  def logger: Logger = Launcher.logger

  def execute(): Unit
  def help(): Unit
}