package com.outr.jefe.repo

import org.powerscala.Version

case class DependencyInfo(dependency: Dependency,
                          latest: VersionedDependency,
                          release: Option[VersionedDependency],
                          versions: List[VersionedDependency]) {
  def version(version: Version): Option[VersionedDependency] = versions.find(_.version == version)

  def merge(other: DependencyInfo): DependencyInfo = {
    if (dependency != other.dependency) throw new RuntimeException(s"Dependencies don't match for merge: $dependency vs ${other.dependency}")
    val latest = if (this.latest.version >= other.latest.version) {
      this.latest
    } else {
      other.latest
    }
    val release = if (this.release.nonEmpty && other.release.nonEmpty) {
      if (this.release.get.version >= other.release.get.version) {
        this.release
      } else {
        other.release
      }
    } else if (this.release.nonEmpty) {
      this.release
    } else {
      other.release
    }
    var versionsMap = Map.empty[String, VersionedDependency]
    this.versions.foreach { vd =>
      versionsMap += vd.version.toString() -> vd
    }
    other.versions.foreach { vd =>
      versionsMap += vd.version.toString() -> vd
    }
    val versions = versionsMap.values.toList.sorted.reverse
    DependencyInfo(dependency, latest, release, versions)
  }
}
