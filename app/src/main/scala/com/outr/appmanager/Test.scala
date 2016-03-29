package com.outr.appmanager

import java.io.File

import com.outr.appmanager.repo._
import com.outr.scribe.formatter.FormatterBuilder
import com.outr.scribe.{LogHandler, Logger, Logging}

object Test extends App with Logging {
  Logger.Root.clearHandlers()
  Logger.Root.addHandler(LogHandler(formatter = FormatterBuilder().date().string(" - ").message.newLine))

  val directory = new File("cache")

  // TODO: support parents in Maven
  // TODO: support version lookup when no version is provided

//  val dependency = "org.scalarelational" %% "scalarelational-core"
  val dependency = "org.scalatest" %% "scalatest"
  val repos = List(Ivy2.Local, Ivy2.Cache, Sonatype.Snapshots, Sonatype.Releases, Maven.Repo1)
  val info = Repository.info(dependency, repos: _*).get
  val version = info.release.get

//  version.dependencies.foreach(d => println(s"Dep: $d / ${d.scope}"))

//  val lookup = "org.scoverage" % "scalac-scoverage-runtime_2.11" % "1.0.1"
//  val updated = lookup.resolve(repos: _*)
//  println(s"Found in repo: ${updated.repository}")

  val resolver = new DependencyResolver(version, repos)
  resolver.resolve()
  logger.info(s"Dependencies: ${resolver.dependencies.keys.mkString(", ")}")
  logger.info(s"Latest: ${resolver.latest.mkString(", ")}")

  val versionDirectory = new File(directory, version.version.toString())
  versionDirectory.mkdirs()
  resolver.download(versionDirectory, latestOnly = true, useCache = true)

//  println(Ivy2.Local.info(scalaRelational))
//  println(Ivy2.Cache.info(scalaTest))
//  val info = Sonatype.Releases.info(scalaRelational).get
//  println(s"Info: $info, ${info.latest.major} / ${info.latest.minor} / ${info.latest.extra}")
}