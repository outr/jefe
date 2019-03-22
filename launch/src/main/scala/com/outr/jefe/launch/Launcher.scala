package com.outr.jefe.launch

import scribe.{Level, Logger}
import scribe.format.Formatter

trait Launcher {
  def name: String
  def loggerId: Long
  def logger: Logger = Logger(loggerId)
  def launch(): Launched
}

object Launcher {
  val loggerId: Long = Logger
    .empty
    .orphan()
    .withHandler(formatter = Formatter.simple, minimumLevel = Some(Level.Info))
    .replace()
    .id
}