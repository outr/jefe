package com.outr.appmanager

import java.io.File

import com.outr.appmanager.repo._

object Test extends App {
  val directory = new File("cache")
  directory.mkdirs()

//  val scalaRelational = "org.scalarelational" %% "scalarelational-core"
  val scalaTest = "org.scalatest" %% "scalatest"
  val info = Repository.info(scalaTest, Ivy2.Local, Ivy2.Cache, Sonatype.Snapshots, Sonatype.Releases)
  println(info)
//  println(Ivy2.Local.info(scalaRelational))
//  println(Ivy2.Cache.info(scalaTest))
//  val info = Sonatype.Releases.info(scalaRelational).get
//  println(s"Info: $info, ${info.latest.major} / ${info.latest.minor} / ${info.latest.extra}")

}