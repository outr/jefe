package com.outr.appmanager

import java.io.File

import com.outr.appmanager.repo._
import org.powerscala.IO

object Test extends App {
  val directory = new File("cache")
  directory.mkdirs()

  val scalaRelational = "org.scalarelational" %% "scalarelational-core"
//  val scalaTest = "org.scalatest" %% "scalatest"
  val repos = List(Ivy2.Local, Ivy2.Cache, Sonatype.Snapshots, Sonatype.Releases)
  val info = Repository.info(scalaRelational, repos: _*).get //, Ivy2.Cache, Sonatype.Snapshots, Sonatype.Releases).get
  val version = info.release.get
  println(s"JAR: ${version.jar}")
  println(version.dependencies)

  val path = version.jar.toString
  val filename = path.substring(path.lastIndexOf('/') + 1)
  IO.copy(version.jar, new File(directory, filename))

  version.dependencies.foreach { d =>
    val dep = d.resolve(repos: _*)
    val path = dep.jar.toString
    val filename = path.substring(path.lastIndexOf('/') + 1)
    IO.copy(dep.jar, new File(directory, filename))
  }
//  println(Ivy2.Local.info(scalaRelational))
//  println(Ivy2.Cache.info(scalaTest))
//  val info = Sonatype.Releases.info(scalaRelational).get
//  println(s"Info: $info, ${info.latest.major} / ${info.latest.minor} / ${info.latest.extra}")

}