package com.outr.jefe.resolve

import java.io.File

import coursier.cache.Cache
import coursier.util.{Gather, Task}
import scribe.Execution.global

object CoursierResolver extends Resolver {
  implicit class CoursierRepository(repository: Repository) {
    def toCoursier: coursier.Repository = repository match {
      case MavenRepository(_, url, credentials) => {
        val auth = credentials.map { c =>
          coursier.core.Authentication(c.user, c.pass)
        }
        coursier.MavenRepository(url, authentication = auth)
      }
      case _: Ivy2Local.type  => coursier.LocalRepositories.ivy2Local
    }
  }

  override protected def resolveInternal(artifact: VersionedArtifact, manager: ArtifactManager): Vector[File] = {
    scribe.info("Resolving dependencies with Coursier...")
    import coursier._
    val start = Resolution().withDependencies(Set(
      Dependency.of(Module(Organization(artifact.group), ModuleName(artifact.name)), artifact.version.toString())
    ))
    val fetch = ResolutionProcess.fetch(manager.repositories.repositories.map(_.toCoursier), Cache.default.fetch)
    val resolution = start.process.run(fetch).unsafeRun()
    resolution.errorCache.foreach {
      case (key, value) => scribe.error(s"Error Cache: $key = $value")
    }
    val errors = resolution.errors
    val conflicts = resolution.conflicts
    assert(resolution.isDone)
    if (errors.nonEmpty) {
      scribe.warn(s"Errors for ($artifact): $errors")
    }
    if (conflicts.nonEmpty) {
      throw new RuntimeException(s"Conflicts for ($artifact): $conflicts")
    }
    val keepTypes = Set("jar", "bundle")
    val localArtifacts = Gather[Task].gather(resolution.artifacts().map(Cache.default.file(_).run)).unsafeRun()
    val fileErrors = localArtifacts.collect {
      case Left(err) => err
    }
    if (fileErrors.nonEmpty) {
      throw new RuntimeException(s"File Errors for ($artifact): $fileErrors")
    }
    localArtifacts.collect {
      case Right(f) => f
    }.toVector
  }
}
