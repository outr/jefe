package com.outr.jefe.resolve

import java.io.File

/**
  * Manages resolution of artifacts
  */
trait Resolver {
  final def resolve(artifact: VersionedArtifact, manager: ArtifactManager): Vector[File] = {
    val versioned = updateVersion(artifact, manager)
    resolveInternal(versioned, manager)
  }

  protected def resolveInternal(artifact: VersionedArtifact, manager: ArtifactManager): Vector[File]

  protected def updateVersion(artifact: VersionedArtifact, manager: ArtifactManager): VersionedArtifact = {
    artifact.version.toString() match {
      case "latest.release" => {
        manager
          .release(artifact.artifact)
          .getOrElse(throw new RuntimeException(s"No latest release found for $artifact"))
      }
      case "latest" | "latest.integration" => {
        manager
          .latest(artifact.artifact)
          .getOrElse(throw new RuntimeException(s"No latest found for $artifact"))
      }
      case _ => artifact
    }
  }
}
