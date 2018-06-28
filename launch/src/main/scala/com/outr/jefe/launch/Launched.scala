package com.outr.jefe.launch

import scribe.Logger

trait Launched {
  private val loggerId: Long = Logger.empty.replace().id
  def logger: Logger = Logger(loggerId)

  def status: ProcessStatus

  def waitForFinished(): ProcessStatus = {
    while (status.isRunning) {
      Thread.sleep(10L)
    }
    status
  }

  def stop(force: Boolean): Unit
}
