package com.outr.appmanager

import java.io.File

import com.outr.appmanager.repo._
import org.powerscala.io._
import scala.collection.mutable

object Test extends App {
  val directory = new File("cache")
  directory.mkdirs()

  val scalaRelational = "org.scalarelational" %% "scalarelational-core"
//  val scalaTest = "org.scalatest" %% "scalatest"
  val repos = List(Ivy2.Local, Ivy2.Cache, Sonatype.Snapshots, Sonatype.Releases, Maven.Repo1)
  val info = Repository.info(scalaRelational, repos: _*).get //, Ivy2.Cache, Sonatype.Snapshots, Sonatype.Releases).get
  val version = info.release.get

  download(version, repos)

//  println(Ivy2.Local.info(scalaRelational))
//  println(Ivy2.Cache.info(scalaTest))
//  val info = Sonatype.Releases.info(scalaRelational).get
//  println(s"Info: $info, ${info.latest.major} / ${info.latest.minor} / ${info.latest.extra}")

  def download(dep: VersionedDependency, repos: List[Repository], cached: mutable.Set[String] = mutable.Set.empty): Unit = {
    val filename = s"${dep.name}-${dep.version}.jar"
    if (cached.contains(filename)) {
      // Already downloaded
      println(s"$filename already downloaded, skipping.")
    } else {
      // TODO: change to find and download, don't bother resolving
      println(s"Downloading $filename...")
      val file = new File(directory, filename)
      IO.stream(dep.jar, file)
      cached += filename

      dep.dependencies.foreach(d => download(d.resolve(repos: _*), repos, cached))
    }
  }
}