package com.outr.jefe.runner

import java.awt.GraphicsEnvironment

import com.outr.jefe.launch.{Launcher, LauncherInstance, LauncherStatus}
import com.outr.jefe.repo._
import com.outr.scribe.Logging

object Runner extends App with Logging {
  val instance = start(loadConfiguration())

  def loadConfiguration(): Configuration = {
    // TODO: remove this
    Configuration.save(Configuration("com.outr.hw" %% "hello-world" % "latest", "com.outr.hw.HelloWorld"))

    // Load configuration
    Configuration.load()
  }

  def start(configuration: Configuration): LauncherInstance = {
    val monitor = if (!configuration.showDialogIfPossible || GraphicsEnvironment.isHeadless) {
      Monitor.Console
    } else {
      Monitor.Dialog
    }
    val manager = DependencyManager(configuration.repositories.list, monitor)
    val files = manager.resolve(configuration.dependency)
    val launcher = new Launcher(configuration.mainClass, files)
    val instance = launcher.classLoaded()
    instance.start()
    instance
  }

  def waitFor(): Unit = {
    while (instance.status.get != LauncherStatus.Finished) {
      Thread.sleep(50)
    }
  }
}