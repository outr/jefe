package com.outr.jefe

import com.outr.jefe.repo._
import com.outr.jefe.launch.Launcher
import scribe.Logging

object Test extends App with Logging {
//  val repositories = List(Ivy2.Local, Ivy2.Cache, Maven.Repo1)
  val repositories = List(Maven.Repo1, Sonatype.Releases)

  val dependency = "com.outr.hw" %% "hello-world" % "latest.release"
  val monitor = Monitor.Dialog
  val manager = DependencyManager(repositories, monitor, useCoursier = true)
  val files = manager.resolve(dependency)

  val launcher = new Launcher("com.outr.hw.HelloWorld", files)
  val instance = launcher.classLoaded()
  instance.status.attach { s =>
    logger.info(s"Status: $s")
  }
  instance.start()
}