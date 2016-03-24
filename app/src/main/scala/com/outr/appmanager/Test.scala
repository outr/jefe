package com.outr.appmanager

import java.io.File

import com.outr.appmanager.repo._

object Test extends App {
  val directory = new File("cache")
  directory.mkdirs()

  val scalaRelational = "org.scalarelational" %% "scalarelational-core"
  println(Ivy2.Local.info(scalaRelational))
//  val info = Sonatype.Releases.info(scalaRelational).get
//  println(s"Info: $info, ${info.latest.major} / ${info.latest.minor} / ${info.latest.extra}")

}