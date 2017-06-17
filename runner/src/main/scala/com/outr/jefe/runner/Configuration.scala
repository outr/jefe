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
                         repositories: Repositories = Repositories(),
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

case class Repositories(list: List[Repository] = List(Ivy2.Local, Maven.Repo1, Sonatype.Releases, Sonatype.Snapshots)) {
  def withMaven(name: String, url: String): Repositories = copy(list ::: List(MavenRepository(name, url)))
}