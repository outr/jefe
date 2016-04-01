package com.outr.appmanager

import java.io.File

import com.outr.appmanager.launch.Launcher
import com.outr.appmanager.repo._
import com.outr.scribe.formatter.FormatterBuilder
import com.outr.scribe.{LogHandler, Logger, Logging}

object Test extends App with Logging {
  Logger.Root.clearHandlers()
  Logger.Root.addHandler(LogHandler(formatter = FormatterBuilder().date().string(" - ").message.newLine))

  val directory = new File("cache")
  val repositories = List(Ivy2.Local, Ivy2.Cache, Maven.Repo1)

  val dependency = "com.outr.hw" %% "hello-world" % "latest.release"
  val monitor = Monitor.Console
  val manager = DependencyManager(repositories, monitor)
//  val latest = manager.latest(dependency)
  val files = manager.resolve(dependency)
  files.foreach(println)

  val launcher = new Launcher("com.outr.hw.HelloWorld", files)
  val launched = launcher.process()
}