package com.outr.jefe.resolve

/**
  * Complete details of an artifact with version
  */
case class VersionedArtifact(group: String, name: String, version: Version) {
  lazy val artifact: Artifact = Artifact(group, name)
}
