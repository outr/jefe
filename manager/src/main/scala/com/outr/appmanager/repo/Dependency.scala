package com.outr.appmanager.repo

import org.powerscala.Version

case class Dependency(group: String, name: String) {
  def %(version: String): VersionedDependency = VersionedDependency(this, Version(version), None, None)
  def versioned(version: Version, scope: Option[String], repository: Repository): VersionedDependency = {
    VersionedDependency(this, version, scope, Option(repository))
  }

  override def toString: String = s"$group % $name"
}
