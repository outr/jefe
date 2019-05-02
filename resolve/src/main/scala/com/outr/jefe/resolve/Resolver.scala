package com.outr.jefe.resolve

import java.io.File

import profig.Profig

/**
  * Manages resolution of artifacts
  */
trait Resolver {
  final def resolve(artifact: VersionedArtifact, manager: ArtifactManager): Vector[File] = {
    val versioned = updateVersion(artifact, manager)
    resolveInternal(versioned, manager)
  }

  // TODO: upgrade to `Future[Either[Throwable, ResolvedArtifact]]`
  protected def resolveInternal(artifact: VersionedArtifact, manager: ArtifactManager): Vector[File]

  protected def updateVersion(artifact: VersionedArtifact, manager: ArtifactManager): VersionedArtifact = {
    artifact.version.toString() match {
      case "latest.release" => {
        val v = manager
          .release(artifact.artifact)
          .getOrElse(throw new RuntimeException(s"No latest release found for $artifact"))
        scribe.info(s"Resolved $v for latest.release")
        v
      }
      case "latest" | "latest.integration" => {
        val v = manager
          .latest(artifact.artifact)
          .getOrElse(throw new RuntimeException(s"No latest found for $artifact"))
        scribe.info(s"Resolved $v for latest.integration")
        v
      }
      case _ => artifact
    }
  }
}

object Resolver {
  def default: Resolver = Profig("resolver").as[String]("sbt") match {
    case "coursier" => CoursierResolver
    case "sbt" => SBTResolver
    case s => {
      scribe.warn(s"Invalid resolver specified: $s (must be 'sbt' or 'coursier'), defaulting to sbt...")
      SBTResolver
    }
  }
}