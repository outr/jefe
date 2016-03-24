package com.outr.appmanager.repo

import org.powerscala.Version

case class VersionedDependency(dependency: Dependency, version: Version, repository: Option[Repository]) extends Ordered[VersionedDependency] {
  override def toString: String = s"$dependency % $version"

  override def compare(that: VersionedDependency): Int = version.compare(that.version)
}