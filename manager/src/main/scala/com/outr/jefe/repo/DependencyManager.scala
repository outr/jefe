package com.outr.jefe.repo

import java.io.File

import scribe.Logging

import scalaz.concurrent.Task

case class DependencyManager(repositories: Seq[Repository], monitor: Monitor = Monitor.Console) extends Logging {
  /**
    * Optionally returns the latest VersionedDependency for the supplied Dependency
    */
  def latest(dependency: Dependency): Option[VersionedDependency] = {
    logger.info(s"Looking up latest version of $dependency...")
    val latest = Repository.info(dependency, repositories).map(_.latest)
    logger.info(s"Latest version: $latest")
    latest
  }

  /**
    * Optionally returns the latest release VersionedDependency for the supplied Dependency
    */
  def release(dependency: Dependency): Option[VersionedDependency] = {
    Repository.info(dependency, repositories).flatMap { info =>
      info.release.orElse(info.versions.find(!_.version.snapshot))
    }
  }

  def resolve(vd: VersionedDependency): Seq[File] = vd.version.toString() match {
    case "latest.release" => resolve(release(vd.dependency).getOrElse(throw new RuntimeException(s"No release available for ${vd.dependency}.")))
    case "latest" | "latest.integration" => resolve(latest(vd.dependency).getOrElse(throw new RuntimeException(s"No version available for ${vd.dependency}.")))
    case _ => {
      import coursier._
      val start = Resolution(Set(Dependency(Module(vd.group, vd.name), vd.version.toString())))
      val fetch = Fetch.from(repositories, Cache.fetch())
      val resolution = start.process.run(fetch).unsafePerformSync
      resolution.errorCache.foreach {
        case (key, value) => scribe.error(s"Error Cache: $key = $value")
      }
//      resolution.dependencies.foreach { d =>
//        scribe.info(s"Dependency: $d")
//      }
      resolution.dependencyArtifacts.foreach {
        case (dependency, artifact) => scribe.info(s"Artifact: $dependency")
      }
      val errors = resolution.metadataErrors
      val conflicts = resolution.conflicts

      if (errors.nonEmpty) {
        logger.warn(s"Errors for ($vd): $errors")
      }
      if (conflicts.nonEmpty) {
        throw new RuntimeException(s"Conflicts for ($vd): $conflicts")
      }
      val localArtifacts = Task.gatherUnordered(
        resolution.artifacts.map(Cache.file(_, logger = Some(monitor.cacheLogger)).run)
      ).unsafePerformSync
      val fileErrors = localArtifacts.map(_.toEither).collect {
        case Left(err) => err
      }
      if (fileErrors.nonEmpty) {
        throw new RuntimeException(s"File Errors for ($vd): $fileErrors")
      }
      localArtifacts.map(_.toEither).collect {
        case Right(f) => f
      }
    }
  }
}