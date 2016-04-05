package com.outr.jefe.runner

import java.io.{File, FileInputStream, FileOutputStream, ObjectInputStream, ObjectOutputStream}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

import com.outr.jefe.repo.{Ivy2, Maven, MavenRepository, Repository, Sonatype, VersionedDependency}

import scala.collection.mutable.ListBuffer

case class Configuration(dependency: VersionedDependency,
                         mainClass: String,
                         showDialogIfPossible: Boolean = true,
                         repositories: Repositories = Repositories())

object Configuration {
  var name: String = "config"

  def load(directory: File = new File(".")): Configuration = {
    val ois = new ObjectInputStream(new GZIPInputStream(new FileInputStream(new File(directory, name))))
    try {
      ois.readObject().asInstanceOf[Configuration]
    } finally {
      ois.close()
    }
  }

  def save(configuration: Configuration, directory: File = new File(".")): Unit = {
    directory.mkdirs()
    if (!directory.isDirectory) throw new RuntimeException(s"Expected directory, but not: ${directory.getAbsolutePath}")
    val oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(new File(directory, name))))
    try {
      oos.writeObject(configuration)
    } finally {
      oos.flush()
      oos.close()
    }
  }
}

case class Repositories(ivyLocal: Boolean = true,
                        ivyCache: Boolean = true,
                        maven: List[(String, String)] = List(Maven.Repo1, Sonatype.Releases, Sonatype.Snapshots).map(_.tupled)) {
  lazy val list: List[Repository] = {
    val b = ListBuffer.empty[Repository]
    if (ivyLocal) b += Ivy2.Local
    if (ivyCache) b += Ivy2.Cache
    b ++= maven.map {
      case (name, baseURL) => MavenRepository(name, baseURL)
    }
    b.toList
  }
}