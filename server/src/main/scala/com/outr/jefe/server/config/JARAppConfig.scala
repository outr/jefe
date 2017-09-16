package com.outr.jefe.server.config

import java.io.File

import com.outr.jefe.launch.{Launcher, ProcessLauncherInstance}
import com.outr.jefe.server.JMXProcessMonitor
import org.powerscala.util.NetUtil

class JARAppConfig(val enabled: Boolean, val jar: File, val mainClass: String, val args: Seq[String], jmxPort: Int, val vmArgs: Seq[String]) extends ProcessApplicationConfig {
  var instance: Option[ProcessLauncherInstance] = None
  var processMonitor: Option[JMXProcessMonitor] = None

  override def start(): Unit = synchronized {
    stop()

    val l = new Launcher(mainClass, Seq(jar), args)
    val li = l.process(jar.getParentFile, vmArgs: _*)
    instance = Some(li)
    processMonitor = Some(new JMXProcessMonitor(jmxPort))
    li.start()
  }

  override def stop(): Unit = synchronized {
    instance match {
      case Some(li) => li.stop()
      case None => // No instance
    }
    instance = None
  }
}