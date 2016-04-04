package com.outr.appmanager

import com.outr.appmanager.launch.Launcher
import com.outr.appmanager.repo._

object Test extends App {
  val repositories = List(Ivy2.Local, Ivy2.Cache, Maven.Repo1)

  val dependency = "com.outr.hw" %% "hello-world" % "latest.release"
  val monitor = Monitor.Console
  val manager = DependencyManager(repositories, monitor)
  val files = manager.resolve(dependency)

  val launcher = new Launcher("com.outr.hw.HelloWorld", files)
  val instance = launcher.classLoaded()
  instance.status.attach { s =>
    println(s"Status: $s")
  }
  instance.start()
}