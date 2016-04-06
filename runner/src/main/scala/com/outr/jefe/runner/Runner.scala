package com.outr.jefe.runner

import java.awt.GraphicsEnvironment

import com.outr.jefe.launch.Launcher
import com.outr.jefe.repo._
import com.outr.scribe.Logging

object Runner extends App with Logging {
  start(loadConfiguration())

  def loadConfiguration(): Configuration = {
    // TODO: remove this
    Configuration.save(Configuration("com.outr.hw" %% "hello-world" % "latest.release", "com.outr.hw.HelloWorld", repositories = Repositories(ivyLocal = false, ivyCache = false, maven = List(Maven.Repo1.tupled))))

    // Load configuration
    Configuration.load()
  }

  def start(configuration: Configuration): Unit = {
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
  }
}