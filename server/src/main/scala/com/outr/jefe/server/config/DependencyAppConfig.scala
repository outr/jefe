package com.outr.jefe.server.config

import java.io.File

import com.outr.jefe.launch.ProcessLauncherInstance
import com.outr.jefe.runner.{Configuration, Repositories, Runner}
import com.outr.jefe.server.JMXProcessMonitor
import com.outr.jefe.repo._
import org.powerscala.util.NetUtil

class DependencyAppConfig(val enabled: Boolean,
                          val workingDirectory: File,
                          val group: String,
                          val artifact: String,
                          val version: String,
                          val mainClass: String,
                          val args: Seq[String],
                          val jmxPort: Int,
                          val vmArgs: Seq[String],
                          val repositories: Repositories,
                          val scalaVersion: Option[String] = Some("2.12")) extends ProcessApplicationConfig {
  var instance: Option[ProcessLauncherInstance] = None
  var processMonitor: Option[JMXProcessMonitor] = None

  override def start(): Unit = synchronized {
    stop()

    val dependency = scalaVersion match {
      case Some(v) => group % s"${artifact}_$v" % version
      case None => group % artifact % version
    }

    scribe.info(s"Starting $dependency...")

    val config = Configuration(dependency, mainClass, args.toArray, workingDirectory = workingDirectory, newProcess = true, vmArgs = vmArgs.toArray)
    val li = Runner.run(config).asInstanceOf[ProcessLauncherInstance]
    instance = Some(li)
    processMonitor = Some(new JMXProcessMonitor(jmxPort))
  }

  override def stop(): Unit = synchronized {
    instance match {
      case Some(li) => li.stop()
      case None => // No instance
    }
    instance = None
  }
}
