package com.outr.appmanager.repo

import java.io.FileNotFoundException
import java.net.URL

import org.powerscala.{IO, Version}

import scala.xml.{Elem, XML}

case class MavenRepository(baseURL: String) extends Repository {
  def info(dependency: Dependency): Option[DependencyInfo] = {
    val url = s"$baseURL/${dependency.group.replace('.', '/')}/${dependency.name}"
    val metadataURL = s"$url/maven-metadata.xml"

    try {
      val metadata = IO.copy(new URL(metadataURL))
      val xml = XML.loadString(metadata)
      val latest = Version((xml \ "versioning" \ "latest").text)
      val release = (xml \ "versioning" \ "release").text match {
        case null | "" => None
        case v => Some(Version(v))
      }
      val versions = (xml \ "versioning" \ "versions" \ "version").toList.map(v => Version(v.text)).sorted.reverse
//      val lastUpdated = (xml \ "versioning" \ "lastUpdated").text

      Some(DependencyInfo(
        dependency = dependency,
        latest = VersionedDependency(dependency, latest, None, this),
        release = release.map(VersionedDependency(dependency, _, None, this)),
        versions = versions.map(VersionedDependency(dependency, _, None, this))
      ))
    } catch {
      case exc: FileNotFoundException => None
    }
  }

  override def jarFor(dependency: VersionedDependency): URL = {
    val d = dependency.dependency
    val url = s"$baseURL/${d.group.replace('.', '/')}/${d.name}/${dependency.version}/${d.name}-${dependency.version}.jar"
    new URL(url)
  }

  override def dependenciesFor(dependency: VersionedDependency): List[VersionedDependency] = {
    val d = dependency.dependency
    val url = s"$baseURL/${d.group.replace('.', '/')}/${d.name}/${dependency.version}/${d.name}-${dependency.version}.pom"
    val xml = XML.load(new URL(url))
    MavenRepository.dependenciesFromPOM(this, xml)
  }
}

object MavenRepository {
  def dependenciesFromPOM(repository: Repository, xml: Elem): List[VersionedDependency] = {
    val dependenciesXML = xml \ "dependencies" \ "dependency"
    dependenciesXML.map { node =>
      val groupId = (node \ "groupId").text
      val artifactId = (node \ "artifactId").text
      val version = (node \ "version").text
      val scope = (node \ "scope").text match {
        case null | "" => None
        case s => Some(s)
      }
      val dependency = Dependency(groupId, artifactId)
      VersionedDependency(dependency, Version(version), scope, repository)
    }.toList
  }
}