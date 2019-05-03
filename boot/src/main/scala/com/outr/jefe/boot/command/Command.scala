package com.outr.jefe.boot.command

import scribe.Logger

trait Command {
  def name: String
  def description: String
  def logger: Logger = scribe.Logger.root

  def execute(): Unit
  def help(): Unit
}