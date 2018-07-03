package com.outr.jefe.application

import com.outr.jefe.launch.JMXProcessMonitor.ProcessStats

trait Application {
  def id: String
  def start(): Unit
  def isRunning: Boolean
  def restart(force: Boolean): Unit
  def stats(): Option[ProcessStats]
  def stop(force: Boolean): Unit
}