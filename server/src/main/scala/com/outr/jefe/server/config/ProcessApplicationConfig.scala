package com.outr.jefe.server.config

import com.outr.jefe.launch.ProcessLauncherInstance
import com.outr.jefe.server.JMXProcessMonitor

trait ProcessApplicationConfig extends ApplicationConfig {
  def instance: Option[ProcessLauncherInstance]
  def processMonitor: Option[JMXProcessMonitor]

  def pid: Option[Int] = instance.map(_.processId)

  def mainClass: String

  def args: Seq[String]
}

object ProcessApplicationConfig {
  def pid(config: ApplicationConfig): Int = config match {
    case pac: ProcessApplicationConfig => pac.pid.getOrElse(-1)
    case _ => -1
  }
}