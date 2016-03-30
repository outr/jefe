package com.outr.appmanager.repo

import java.io.FileNotFoundException
import java.net.URL

import org.powerscala.Version
import org.powerscala.io._

import scala.xml.XML

case class MavenRepository(name: String, baseURL: String) extends Repository {
  lazy val internal = coursier.MavenRepository(baseURL)

  def info(dependency: Dependency): Option[DependencyInfo] = {
    val url = s"$baseURL/${dependency.group.replace('.', '/')}/${dependency.name}"
    val metadataURL = s"$url/maven-metadata.xml"

    try {
      val metadata = IO.stream(new URL(metadataURL), new StringBuilder).toString
      val xml = XML.loadString(metadata)
      val latest = (xml \ "versioning" \ "latest").text match {
        case "" | null => None
        case Version(v) => Some(v)
      }
      val release = (xml \ "versioning" \ "release").text match {
        case null | "" => None
        case Version(v) => Some(v)
      }
      val versions = (xml \ "versioning" \ "versions" \ "version").toList.map(_.text).collect {
        case Version(v) => v
      }.sorted.reverse
//      val lastUpdated = (xml \ "versioning" \ "lastUpdated").text

      Some(DependencyInfo(
        dependency = dependency,
        latest = VersionedDependency(dependency, latest.getOrElse(versions.find(!_.snapshot).getOrElse(versions.head)), None, Some(this)),
        release = release.map(VersionedDependency(dependency, _, None, Some(this))),
        versions = versions.map(VersionedDependency(dependency, _, None, Some(this)))
      ))
    } catch {
      case exc: FileNotFoundException => None
      case t: Throwable => throw new RuntimeException(s"Failed to process maven metadata: $metadataURL.", t)
    }
  }

  override def toString: String = name
}