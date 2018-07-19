package com.outr.jefe.launch

import com.outr.jefe.launch.jmx.ProcessStats
import scribe.Logger

trait Launched {
  private val loggerId: Long = Logger.empty.withParent(launcher.loggerId).replace().id

  def launcher: Launcher
  def logger: Logger = Logger(loggerId)

  def status: ProcessStatus

  def waitForFinished(): ProcessStatus = {
    while (status.isRunning) {
      Thread.sleep(10L)
    }
    status
  }

  def stats(): Option[ProcessStats] = None

  def stop(force: Boolean): Unit
}
