package com.outr.jefe.boot.command

import com.outr.jefe.boot.JefeBoot
import scribe.Logger

trait Command {
  def name: String
  def description: String
  def logger: Logger = JefeBoot.logger

  def execute(): Unit
  def help(): Unit
}