package com.outr.appmanager

import java.io.File

object Test extends App {
  val directory = new File("cache")
  directory.mkdirs()

  val manager = MavenManager()
  val scalaRelational = Dependency("org.scalarelational", "scalarelational-core_2.11")
  val instance = DependencyInstance(scalaRelational, manager)
  println(s"Latest: ${instance.latestVersion}")
}
