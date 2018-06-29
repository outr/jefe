package com.outr.jefe.resolve

import java.io.{File, FileNotFoundException}
import java.net.URL

import org.powerscala.io.IO

import scala.xml.XML

sealed trait Repository {
  def info(artifact: Artifact): Option[ArtifactDetails]
}

object Ivy2Local extends Repository {
  private val baseDirectory = new File(s"${System.getProperty("user.home")}/.ivy2/local")

  override def info(artifact: Artifact): Option[ArtifactDetails] = {
    val directory = new File(baseDirectory, s"${artifact.group}/${artifact.name}")
    if (directory.exists()) {
      val directoryNames = directory.listFiles().toList.filter(f => f.isDirectory).map(_.getName)
      val versions = directoryNames.collect {
        case Version(v) => v
      }.sorted.reverse
      val latest = versions.headOption
      val release = versions.find(!_.snapshot)
      Some(ArtifactDetails(artifact, latest, release, versions))
    } else {
      None
    }
  }
}

case class MavenRepository(name: String, url: String) extends Repository {
  def info(artifact: Artifact): Option[ArtifactDetails] = {
    val artifactURL = s"$url/${artifact.group.replace('.', '/')}/${artifact.name}"
    val metadataURL = s"$artifactURL/maven-metadata.xml"

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

      Some(ArtifactDetails(
        artifact = artifact,
        latest = latest,
        release = release,
        versions = versions
      ))
    } catch {
      case _: FileNotFoundException => None
      case t: Throwable => throw new RuntimeException(s"Failed to process maven metadata: $metadataURL.", t)
    }
  }
}