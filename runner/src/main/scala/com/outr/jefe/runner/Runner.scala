package com.outr.jefe.runner

import java.awt.GraphicsEnvironment

import com.outr.jefe.launch.{Launcher, LauncherInstance, LauncherStatus}
import com.outr.jefe.repo._
import com.outr.scribe.Logging

object Runner extends Logging {
  def main(args: Array[String]): Unit = {
    val map = args.collect {
      case s if s.indexOf('=') != 1 => s.substring(0, s.indexOf('=')) -> s.substring(s.indexOf('=') + 1)
    }.toMap
    if (!map.contains("groupId") || !map.contains("artifactId") || !map.contains("mainClass")) {
      fail("Usage: java -jar runner.jar groupId=com.company artifactId=project mainClass=com.company.MyClass")
    }
    val groupId = map("groupId")
    val artifactId = map("artifactId")
    val mainClass = map("mainClass")
    val configuration = Configuration(groupId %% artifactId % "latest", mainClass)
    run(configuration)
  }

  def run(configuration: Configuration): LauncherInstance = {
    start(configuration)
  }

  def loadConfiguration(): Configuration = {
    // TODO: remove this
    Configuration.save(Configuration("com.outr.hw" %% "hello-world" % "latest", "com.outr.hw.HelloWorld"))

    // Load configuration
    Configuration.load()
  }

  private def fail(message: String): Unit = {
    System.err.println(message)
    System.exit(1)
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

  def waitFor(instance: LauncherInstance): Unit = {
    while (instance.status.get != LauncherStatus.Finished) {
      Thread.sleep(50)
    }
  }
}