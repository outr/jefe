package com.outr.appmanager.repo

import org.powerscala.Version

case class Dependency(group: String, name: String) {
  def %(version: Version, scope: Option[String], repository: Repository): VersionedDependency = {
    VersionedDependency(this, version, scope, repository)
  }

  override def toString: String = s"$group % $name"
}
