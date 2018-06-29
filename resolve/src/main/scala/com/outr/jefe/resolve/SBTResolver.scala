package com.outr.jefe.resolve

import java.io.File

import sbt.internal.util.ConsoleLogger
import sbt.librarymanagement.ModuleID
import sbt.librarymanagement.ivy.{InlineIvyConfiguration, IvyDependencyResolution, IvyPaths}
import sbt.util.Level

object SBTResolver extends Resolver {
  private lazy val log = {
    val l = ConsoleLogger(System.out)
    l.setLevel(Level.Info)
    l
  }

  implicit class SBTRepository(repository: Repository) {
    def toSBT: sbt.librarymanagement.Resolver = repository match {
      case MavenRepository(name, url) => sbt.librarymanagement.MavenRepository(name, url)
      case _: Ivy2Local.type => sbt.librarymanagement.Resolver.defaultLocal
    }
  }

  override protected def resolveInternal(artifact: VersionedArtifact, manager: ArtifactManager): Vector[File] = {
    scribe.info("Resolving dependencies with SBT...")
    val baseDirectory = new File(".")
    val resolvers: Vector[sbt.librarymanagement.Resolver] = manager.repositories.repositories.map(_.toSBT).toVector
    val paths = IvyPaths(baseDirectory, new File(System.getProperty("user.home"), ".ivy2"))
    val config = InlineIvyConfiguration()
      .withLog(log)
      .withPaths(paths)
      .withResolvers(resolvers)
    val lm = IvyDependencyResolution(config)
    val module = ModuleID(artifact.group, artifact.name, artifact.version.toString())
    val retrieveDirectory = new File(baseDirectory, ".cache")
    lm.retrieve(module, scalaModuleInfo = None, retrieveDirectory = retrieveDirectory, log = log) match {
      case Left(unresolvedWarning) => throw unresolvedWarning.resolveException
      case Right(files) => files
    }
  }
}
