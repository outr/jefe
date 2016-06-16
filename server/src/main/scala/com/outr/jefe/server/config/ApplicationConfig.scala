package com.outr.jefe.server.config

import com.outr.jefe.launch.ProcessLauncherInstance
import com.outr.jefe.server.JMXProcessMonitor

trait ApplicationConfig {
  def instance: Option[ProcessLauncherInstance]
  def processMonitor: Option[JMXProcessMonitor]

  def pid: Option[Int] = instance.map(_.processId)

  def enabled: Boolean

  def mainClass: String

  def args: Seq[String]

  def start(): Unit

  def stop(): Unit
}
