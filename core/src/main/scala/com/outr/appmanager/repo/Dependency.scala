package com.outr.appmanager.repo

import org.powerscala.Version

case class Dependency(group: String, name: String) {
  def %(version: Version, repository: Option[Repository]): VersionedDependency = {
    VersionedDependency(this, version, repository)
  }

  override def toString: String = s"$group % $name"
}
