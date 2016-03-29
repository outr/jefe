package com.outr.appmanager.repo

import java.io.FileNotFoundException
import java.net.{HttpURLConnection, URL}

import com.outr.scribe.Logging
import org.powerscala.Version
import org.powerscala.io._

import scala.xml.{Elem, XML}

case class MavenRepository(name: String, baseURL: String) extends Repository {
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

  override def jarFor(dependency: VersionedDependency): URL = {
    val d = dependency.dependency
    val url = s"$baseURL/${d.group.replace('.', '/')}/${d.name}/${dependency.version}/${d.name}-${dependency.version}.jar"
    new URL(url)
  }

  override def hasVersion(dependency: VersionedDependency): Boolean = {
    val url = s"$baseURL/${dependency.group.replace('.', '/')}/${dependency.name}/${dependency.version}"
    val pomURL = new URL(s"$url/${dependency.name}-${dependency.version}.pom")
    val connection = pomURL.openConnection().asInstanceOf[HttpURLConnection]
    try {
      connection.setRequestMethod("GET")
      connection.connect()
      connection.getResponseCode == 200
    } finally {
      connection.disconnect()
    }
  }

  override def dependenciesFor(dependency: VersionedDependency): (Option[VersionedDependency], List[VersionedDependency]) = {
    val d = dependency.dependency
    val url = s"$baseURL/${d.group.replace('.', '/')}/${d.name}/${dependency.version}/${d.name}-${dependency.version}.pom"
    val xml = XML.load(new URL(url))
    try {
      MavenRepository.dependenciesFromPOM(this, xml)
    } catch {
      case t: Throwable => throw new RuntimeException(s"Error parsing dependencies from $url", t)
    }
  }

  override def toString: String = name
}

object MavenRepository extends Logging {
  private val PropertyRegex = """\$\{(.+)\}""".r

  def dependenciesFromPOM(repository: Repository, xml: Elem): (Option[VersionedDependency], List[VersionedDependency]) = {
    var properties: Map[String, String] = (xml \ "properties").headOption match {
      case Some(props) => props.asInstanceOf[Elem].child.map(n => n.label -> n.text).toMap
      case None => Map.empty
    }

    // Add repositories
    (xml \ "repositories" \ "repository").foreach { n =>
//      val id = (n \ "id").text
      val name = (n \ "name").text
//      val layout = (n \ "layout").text
      val url = (n \ "url").text
      val repo = MavenRepository(name, url)
      DependencyResolver.get().foreach(dr => dr.add(repo))
    }

    val parentXML = (xml \ "parent").headOption
    val parent = parentXML.map { p =>
      val groupId = (p \ "groupId").text
      val artifactId = (p \ "artifactId").text
      val version = (p \ "version").text
      VersionedDependency(Dependency(groupId, artifactId), Version(version), scope = None, repository = Some(repository))
    }
    parent.foreach { p =>
      properties += "project.version" -> p.version.toString()
    }
    val dependenciesXML = xml \ "dependencies" \ "dependency" match {
      case nodes if nodes.nonEmpty => nodes
      case _ => xml \ "dependencyManagement" \ "dependencies" \ "dependency"
    }
    val dependencies = dependenciesXML.map { node =>
      val groupId = (node \ "groupId").text
      val artifactId = (node \ "artifactId").text
      val version = (node \ "version").text match {
        case PropertyRegex(key) => properties.getOrElse(key, {
          logger.warn(s"Unable to find version because of missing property: $key for $groupId % $artifactId")
          ""
        })
        case v => v
      }
      val optional = (node \ "optional").text match {
        case "true" => true
        case _ => false
      }
      val scope = (node \ "scope").text match {
        case null | "" => if (optional) {
          Some("optional")
        } else {
          None
        }
        case s => Some(s)
      }
      val ver = version match {
        case Version(v) => v
        case _ => Version.Zero
      }
      VersionedDependency(Dependency(groupId, artifactId), ver, scope, Option(repository))
    }.toList
    (parent, dependencies)
  }
}