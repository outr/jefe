package com.outr.jefe.resolve

import java.io.File

case class ArtifactManager(repositories: Repositories, resolver: Resolver) {
  /**
    * Optionally returns the latest VersionedArtifact for the supplied Artifact
    */
  def latest(artifact: Artifact): Option[VersionedArtifact] = {
    scribe.info(s"Looking up latest version of $artifact...")
    val latest = repositories.info(artifact).flatMap(_.latest).map(artifact % _)
    scribe.info(s"Latest version: $latest")
    latest
  }

  /**
    * Optionally returns the latest release VersionedDependency for the supplied Dependency
    */
  def release(artifact: Artifact): Option[VersionedArtifact] = {
    repositories.info(artifact).flatMap { info =>
      info.release.orElse(info.versions.find(!_.snapshot))
    }.map(artifact % _)
  }

  def resolve(artifact: VersionedArtifact): Vector[File] = resolver.resolve(artifact, this)
}
