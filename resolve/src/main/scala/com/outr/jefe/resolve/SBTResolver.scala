package com.outr.jefe.resolve

import java.io.File

import org.apache.ivy.util.url.CredentialsStore
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

  private lazy val HostRegex = """\S+//(.+?)/.+""".r

  implicit class SBTRepository(repository: Repository) {
    def toSBT: sbt.librarymanagement.Resolver = repository match {
      case MavenRepository(name, url, credentials) => {
        credentials.foreach { c =>
          val host = url match {
            case HostRegex(h) => h
            case _ => throw new RuntimeException(s"Unparsable url: $url")
          }
          CredentialsStore.INSTANCE.addCredentials(name, host, c.user, c.pass)
        }
        sbt.librarymanagement.MavenRepository(name, url)
      }
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