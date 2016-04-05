package com.outr.jefe.repo

import com.outr.scribe.Logging
import org.powerscala.Version

case class VersionedDependency(dependency: Dependency, version: Version, scope: Option[String], repository: Option[Repository]) extends Ordered[VersionedDependency] with Logging {
  def group: String = dependency.group
  def name: String = dependency.name
  lazy val string: String = s"$dependency % $version"

  def %(scope: String): VersionedDependency = copy(scope = Option(scope))

  override def toString: String = scope match {
    case Some(s) => s"$dependency % $version % $s"
    case None => string
  }

  override def compare(that: VersionedDependency): Int = version.compare(that.version)
}