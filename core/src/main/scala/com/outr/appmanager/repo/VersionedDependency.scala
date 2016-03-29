package com.outr.appmanager.repo

import java.net.URL

import com.outr.scribe.Logging
import org.powerscala.Version

case class VersionedDependency(dependency: Dependency, version: Version, scope: Option[String], repository: Option[Repository]) extends Ordered[VersionedDependency] with Logging {
  def group: String = dependency.group
  def name: String = dependency.name
  lazy val jar: URL = repository.get.jarFor(this)
  lazy val (parent: Option[VersionedDependency], dependencies: List[VersionedDependency]) = repository.get.dependenciesFor(this)
  lazy val string: String = s"$dependency % $version"

  def %(scope: String): VersionedDependency = copy(scope = Option(scope))

  def resolve(repositories: Repositories): VersionedDependency = {
    if (version == Version.Zero) {
      // TODO: can't lookup parent without version, can't determine version without parent
      parent match {
        case Some(p) => {
          p.dependencies.find(vd => vd.group == dependency.group) match {
            case Some(d) => copy(version = d.version).resolve(repositories)
            case None => throw new RuntimeException(s"Version undefined for $dependency. Unable to find version for ${dependency.group} in $p.")
          }
        }
        case None => throw new RuntimeException(s"Version undefined for $dependency and no parent.")
      }
//      DependencyResolver.get().flatMap(dr => dr.versionForGroupId(dependency.group)) match {
//        case Some(v) => copy(version = v).resolve(repositories)
//        case None => throw new RuntimeException(s"Version undefined for $dependency")
//      }
    } else {
      repositories.find { repo =>
        repo.hasVersion(this)
      } match {
        case Some(resolved) => copy(repository = Option(resolved))
        case None => throw new RuntimeException(s"Unable to find $string in any repository.")
      }
    }


//    val info = Repository.info(dependency, repositories: _*).getOrElse(throw new RuntimeException(s"Unable to find: $dependency"))
//    info.version(version) match {
//      case Some(vd) => vd
//      case None => {
//        logger.warn(s"Unable to find version in repositories for $this. Presuming mistake in POM file.")
//        info.versions.head.copy(version = version)
//      }
//    }
  }

  override def toString: String = scope match {
    case Some(s) => s"$dependency % $version % $s"
    case None => string
  }

  override def compare(that: VersionedDependency): Int = version.compare(that.version)
}