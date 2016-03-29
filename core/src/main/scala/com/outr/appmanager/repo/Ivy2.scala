package com.outr.appmanager.repo

import java.io.File
import java.net.URL

import org.powerscala.Version

import scala.xml.XML

object Ivy2 {
  object Local extends Repository {
    private val baseDirectory = new File(s"${System.getProperty("user.home")}/.ivy2/local")

    override def info(dependency: Dependency): Option[DependencyInfo] = {
      val directory = new File(baseDirectory, s"${dependency.group}/${dependency.name}")
      if (directory.exists()) {
        val directoryNames = directory.listFiles().toList.filter(f => f.isDirectory).map(_.getName)
        val versions = directoryNames.collect {
          case Version(v) => VersionedDependency(dependency, v, None, Some(this))
        }.sorted.reverse
        val latest = versions.head
        val release = versions.find(!_.version.snapshot)
        Some(DependencyInfo(dependency, latest, release, versions))
      } else {
        None
      }
    }

    override def jarFor(dependency: VersionedDependency): URL = {
      val file = new File(baseDirectory, s"${dependency.dependency.group}/${dependency.dependency.name}/${dependency.version}/jars/${dependency.dependency.name}.jar")
      file.toURI.toURL
    }

    override def dependenciesFor(dependency: VersionedDependency): (Option[VersionedDependency], List[VersionedDependency]) = {
      val file = new File(baseDirectory, s"${dependency.dependency.group}/${dependency.dependency.name}/${dependency.version}/poms/${dependency.dependency.name}.pom")
      val xml = XML.loadFile(file)
      MavenRepository.dependenciesFromPOM(this, xml)
    }

    override def hasVersion(dependency: VersionedDependency): Boolean = {
      val directory = new File(baseDirectory, s"${dependency.group}/${dependency.name}/${dependency.version}")
      directory.exists()
    }

    override def toString: String = "Ivy2.Local"
  }
  object Cache extends Repository {
    private val baseDirectory = new File(s"${System.getProperty("user.home")}/.ivy2/cache")

    override def info(dependency: Dependency): Option[DependencyInfo] = {
      val directory = new File(baseDirectory, s"${dependency.group}/${dependency.name}")
      if (directory.exists()) {
        val xmlNames = directory.listFiles().toList.map(_.getName).filter(s => s.startsWith("ivy-") && s.endsWith(".xml")).map(s => s.substring(4, s.length() - 4))
        val versions = xmlNames.collect {
          case Version(v) => VersionedDependency(dependency, v, None, Some(this))
        }.sorted.reverse
        val latest = versions.head
        val release = versions.find(!_.version.snapshot)
        Some(DependencyInfo(dependency, latest, release, versions))
      } else {
        None
      }
    }

    override def jarFor(dependency: VersionedDependency): URL = {
      val file = new File(baseDirectory, s"${dependency.dependency.group}/${dependency.dependency.name}/jars/${dependency.name}-${dependency.version}.jar")
      file.toURI.toURL
    }

    override def dependenciesFor(dependency: VersionedDependency): (Option[VersionedDependency], List[VersionedDependency]) = {
      val file = new File(baseDirectory, s"${dependency.dependency.group}/${dependency.dependency.name}/ivy-${dependency.version}.xml")
      val xml = XML.loadFile(file)
      val dependenciesXML = xml \ "dependencies" \ "dependency"
      val dependencies = dependenciesXML.map { n =>
        val org = (n \ "@org").text
        val name = (n \ "@name").text
        val rev = (n \ "@rev").text
        val conf = (n \ "@conf").text
        val dependency = Dependency(org, name)
        val version = Version(rev)
        val scope = if (conf.contains("runtime->runtime(*)")) {
          None
        } else {
          Some(conf.substring(0, conf.indexOf('-')))
        }
        VersionedDependency(dependency, version, scope, Some(this))
      }.toList
      (None, dependencies)
    }

    override def hasVersion(dependency: VersionedDependency): Boolean = {
      val directory = new File(baseDirectory, s"${dependency.group}/${dependency.name}/jars/${dependency.name}-${dependency.version}.jar")
      directory.exists()
    }

    override def toString: String = "Ivy2.Cache"
  }
}