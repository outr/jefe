package com.outr.appmanager.repo

import java.net.URL

import com.outr.scribe.Logging
import org.powerscala.Version

case class VersionedDependency(dependency: Dependency, version: Version, scope: Option[String], repository: Repository) extends Ordered[VersionedDependency] with Logging {
  def group: String = dependency.group
  def name: String = dependency.name
  lazy val jar: URL = repository.jarFor(this)
  lazy val dependencies: List[VersionedDependency] = repository.dependenciesFor(this)

  def resolve(repositories: Repository*): VersionedDependency = {
    val info = Repository.info(dependency, repositories: _*).getOrElse(throw new RuntimeException(s"Unable to find: $dependency"))
    info.version(version) match {
      case Some(vd) => vd
      case None => {
        logger.warn(s"Unable to find version in repositories for $this. Presuming mistake in POM file.")
        info.versions.head.copy(version = version)
      }
    }
  }

  override def toString: String = scope match {
    case Some(s) => s"$dependency % $version % $s"
    case None => s"$dependency % $version"
  }

  override def compare(that: VersionedDependency): Int = version.compare(that.version)
}