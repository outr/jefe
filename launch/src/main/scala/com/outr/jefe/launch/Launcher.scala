package com.outr.jefe.launch

import scribe.Logger

trait Launcher {
  def name: String
  def loggerId: Long
  def logger: Logger = Logger(loggerId)
  def launch(): Launched
}