package com.outr.appmanager.repo

import java.io.FileNotFoundException
import java.net.URL

import org.powerscala.{IO, Version}

import scala.xml.XML

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
        latest = VersionedDependency(dependency, latest, Some(this)),
        release = release.map(VersionedDependency(dependency, _, Some(this))),
        versions = versions.map(VersionedDependency(dependency, _, Some(this)))
      ))
    } catch {
      case exc: FileNotFoundException => None
    }
  }
}
