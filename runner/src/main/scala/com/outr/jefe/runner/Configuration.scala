package com.outr.jefe.runner

import java.io.{File, FileInputStream, FileOutputStream, ObjectInputStream, ObjectOutputStream}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

import com.outr.jefe.repo._

import scala.collection.mutable.ListBuffer

case class Configuration(dependency: VersionedDependency,
                         mainClass: String,
                         args: Array[String] = Array.empty,
                         vmArgs: Array[String] = Array.empty,
                         workingDirectory: File = new File("."),
                         showDialogIfPossible: Boolean = true,
                         repositories: Repositories = Repositories.simple(),
                         newProcess: Boolean = false)

object Configuration {
  def load(file: File): Configuration = {
    val ois = new ObjectInputStream(new GZIPInputStream(new FileInputStream(file)))
    try {
      ois.readObject().asInstanceOf[Configuration]
    } finally {
      ois.close()
    }
  }

  def save(configuration: Configuration, file: File): Unit = {
    Option(file.getParentFile).foreach(_.mkdirs())
    val oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(file)))
    try {
      oos.writeObject(configuration)
    } finally {
      oos.flush()
      oos.close()
    }
  }
}

case class Repositories(list: List[Repository]) {
  def withMaven(name: String, url: String): Repositories = copy(list ::: List(MavenRepository(name, url)))
}

object Repositories {
  def simple(ivyLocal: Boolean = true,
             ivyCache: Boolean = true,
             maven: List[(String, String)] = List(Maven.Repo1, Sonatype.Releases, Sonatype.Snapshots).map(_.tupled)): Repositories = {
    val b = ListBuffer.empty[Repository]
    if (ivyLocal) b += Ivy2.Local
    if (ivyCache) b += Ivy2.Cache
    b ++= maven.map {
      case (name, baseURL) => MavenRepository(name, baseURL)
    }
    Repositories(b.toList)
  }
}