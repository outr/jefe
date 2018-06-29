package com.outr.jefe.resolve

import java.io.File

object CoursierResolver extends Resolver {
  implicit class CoursierRepository(repository: Repository) {
    def toCoursier: coursier.Repository = repository match {
      case MavenRepository(_, url) => coursier.MavenRepository(url)
      case _: Ivy2Local.type  => coursier.Cache.ivy2Local
    }
  }

  override protected def resolveInternal(artifact: VersionedArtifact, manager: ArtifactManager): Vector[File] = {
    scribe.info("Resolving dependencies with Coursier...")
    import coursier._
    val start = Resolution(Set(Dependency(Module(artifact.group, artifact.name), artifact.version.toString(), "runtime")))
    val fetch = Fetch.from(manager.repositories.repositories.map(_.toCoursier), Cache.fetch())
    val resolution = start.process.run(fetch).unsafePerformSync
    resolution.errorCache.foreach {
      case (key, value) => scribe.error(s"Error Cache: $key = $value")
    }
    val errors = resolution.metadataErrors
    val conflicts = resolution.conflicts
    assert(resolution.isDone)
    if (errors.nonEmpty) {
      scribe.warn(s"Errors for ($artifact): $errors")
    }
    if (conflicts.nonEmpty) {
      throw new RuntimeException(s"Conflicts for ($artifact): $conflicts")
    }
    val keepTypes = Set("jar", "bundle")
    val localArtifacts = scalaz.concurrent.Task.gatherUnordered(
      resolution.dependencyArtifacts.map(_._2).filter(a => keepTypes(a.`type`)).map(Cache.file(_, logger = None).run)
    ).unsafePerformSync
    val fileErrors = localArtifacts.map(_.toEither).collect {
      case Left(err) => err
    }
    if (fileErrors.nonEmpty) {
      throw new RuntimeException(s"File Errors for ($artifact): $fileErrors")
    }
    localArtifacts.map(_.toEither).collect {
      case Right(f) => f
    }.toVector
  }
}
