package com.outr.appmanager

import java.io.File
import java.net.URLClassLoader

import com.outr.appmanager.repo._
import com.outr.scribe.formatter.FormatterBuilder
import com.outr.scribe.{LogHandler, Logger, Logging}

import scalaz.concurrent.Task

object Test extends App with Logging {
  Logger.Root.clearHandlers()
  Logger.Root.addHandler(LogHandler(formatter = FormatterBuilder().date().string(" - ").message.newLine))

  val directory = new File("cache")
  val repositories = List(Ivy2.Local, Ivy2.Cache, Maven.Repo1)

//  testCoursier("org.scalarelational", "scalarelational-core_2.11", "[1.0,)", repositories)
//  testCoursier("com.cedarsoft.commons", "image", "7.1.0")
  testCoursier("com.outr.hw", "hello-world_2.11", "latest.release", repositories)

//  testIvy("log4j", "log4j", "1.2.16")
//  testIvy("org.scalatest", "scalatest_2.11", "latest.release")
//  testIvy("org.scalarelational", "scalarelational-core_2.11", "1.3.0")
//  testIvy("com.outr.hw", "hello-world_2.11", "1.0.0-SNAPSHOT")

  def testCoursier(groupId: String, artifactId: String, version: String, repositories: List[Repository]): Unit = version match {
    case "latest.release" => Repository.info(groupId % artifactId, repositories) match {
      case Some(info) => {
        info.release.orElse(info.versions.find(!_.version.snapshot)) match {
          case Some(vd) => testCoursier(groupId, artifactId, vd.version.toString, repositories)
          case None => throw new RuntimeException(s"Unable to find a released version of $groupId % $artifactId.")
        }
      }
      case None => throw new RuntimeException(s"Unable to find information in repositories for $groupId % $artifactId.")
    }
    case "latest" | "latest.integration" => Repository.info(groupId % artifactId, repositories) match {
      case Some(info) => testCoursier(groupId, artifactId, info.latest.version.toString, repositories)
      case None => throw new RuntimeException(s"Unable to find information in repositories for $groupId % $artifactId.")
    }
    case _ => {
      import coursier._
      val start = Resolution(Set(Dependency(Module(groupId, artifactId), version)))
      //    val repositories = Seq(Cache.ivy2Local, Cache.ivy2Cache, MavenRepository("https://repo1.maven.org/maven2"))
      val fetch = Fetch.from(repositories, Cache.fetch())
      val resolution = start.process.run(fetch).run
      val errors = resolution.errors
      val conflicts = resolution.conflicts

      if (errors.nonEmpty) {
        throw new RuntimeException(s"Errors: $errors")
      }
      if (conflicts.nonEmpty) {
        throw new RuntimeException(s"Conflicts: $conflicts")
      }

      val localArtifacts = Task.gatherUnordered(
        resolution.artifacts.map(Cache.file(_).run)
      ).run
      val fileErrors = localArtifacts.map(_.toEither).collect {
        case Left(err) => err
      }
      if (fileErrors.nonEmpty) {
        throw new RuntimeException(s"File Errors: $fileErrors")
      }
      val files = localArtifacts.map(_.toEither).collect {
        case Right(f) => f
      }
      println(s"Files: ${files.map(_.getAbsolutePath).mkString(", ")}")
      /*val classLoader = new URLClassLoader(files.map(_.toURI.toURL).toArray, null)
    val c = classLoader.loadClass("com.outr.hw.HelloWorld")
    println(s"Class: $c")
    val main = c.getMethod("main", classOf[Array[String]])
    println(s"Main: $main")
    main.invoke(null, Array[String]())*/
    }
  }
}