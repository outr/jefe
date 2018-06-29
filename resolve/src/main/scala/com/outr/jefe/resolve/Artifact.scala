package com.outr.jefe.resolve

/**
  * General information about an artifact
  */
case class Artifact(group: String, name: String) {
  def withVersion(version: Version): VersionedArtifact = VersionedArtifact(group, name, version)
  def %(version: Version): VersionedArtifact = withVersion(version)

  override def toString: String = s"$group % $name"
}