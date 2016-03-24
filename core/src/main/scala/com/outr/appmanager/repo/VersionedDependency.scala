package com.outr.appmanager.repo

import java.net.URL

import org.powerscala.Version

case class VersionedDependency(dependency: Dependency, version: Version, scope: Option[String], repository: Repository) extends Ordered[VersionedDependency] {
  lazy val jar: URL = repository.jarFor(this)
  lazy val dependencies: List[VersionedDependency] = repository.dependenciesFor(this)

  override def toString: String = scope match {
    case Some(s) => s"$dependency % $version % $s"
    case None => s"$dependency % $version"
  }

  override def compare(that: VersionedDependency): Int = version.compare(that.version)
}