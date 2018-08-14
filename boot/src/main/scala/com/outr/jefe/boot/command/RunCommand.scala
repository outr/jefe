package com.outr.jefe.boot.command

import java.io.File

import com.outr.jefe.application.ProcessApplication
import com.outr.jefe.boot.JefeBoot
import profig.Profig
import com.outr.jefe.resolve._
import org.powerscala.io.IO

object RunCommand extends Command {
  private val MavenVersionedRegex = """(.+)[:](.+)[:](.+)""".r
  private val MavenRegex = """(.+)[:](.+)""".r

  override def name: String = "run"
  override def description: String = "Runs an ad-hoc process"

  override def execute(): Unit = {
    Profig("arg2").as[Option[String]] match {
      case Some(applicationInfo) => {
        val artifact: VersionedArtifact = applicationInfo match {
          case MavenVersionedRegex(groupId, artifactId, version) => {
            groupId % artifactId % version
          }
          case MavenRegex(groupId, artifactId) => {
            val version = Profig("version").as[Option[String]].getOrElse(
              JefeBoot.config(s"$groupId.$artifactId.version").as[Option[String]].getOrElse("latest.release")
            )
            groupId % artifactId % version
          }
        }
        val mainClass = Profig("mainClass").as[Option[String]].orElse(
          JefeBoot.config(s"${artifact.group}.${artifact.name}.mainClass").as[Option[String]]
        )
        val workingDirectory = new File(Profig("workingDirectory").as[Option[String]].getOrElse("."))
        val jvmArgs = loadArgs(workingDirectory, ".jvmopts")
        val args = loadArgs(workingDirectory, ".args")
        val repositories = JefeBoot.repositories
        val application = ProcessApplication.artifact(
          id = artifact.name,
          artifacts = List(artifact),
          repositories = repositories,
          mainClass = mainClass,
          jvmArgs = jvmArgs,
          args = args,
          workingDirectory = workingDirectory
        )
        application.start()
        application.waitForFinished()
      }
      case None => {
        logger.info("jefe: missing argument")
        help()
      }
    }
  }

  def loadArgs(directory: File, fileName: String): List[String] = {
    var file = new File(directory, fileName)
    if (!file.exists()) {
      file = new File(fileName)
    }
    if (file.exists()) {
      IO.stream(file, new StringBuilder).toString.split("\n").map(_.trim).filter(_.nonEmpty).toList
    } else {
      Nil
    }
  }

  override def help(): Unit = {
    logger.info("Usage: jefe run groupId:artifactId(:version)")
    logger.info("")
    logger.info("You must specify a proper Maven dependency to run.")
    logger.info("If you exclude the version, a Jefe setting of groupId.artifactId.version will be used if specified. Otherwise, the latest release will be used.")
    logger.info("For the version you can also specify 'latest.release' for the latest release or 'latest' for latest integration.")
    logger.info("JVM Arguments can be provided via '.jvmopts' file found in the current directory or the 'workingDirectory'.")
    logger.info("Arguments can be provided via '.args' file found in the current directory of the 'workingDirectory'.")
    logger.info("By default, blocks until the process is terminated")
    logger.info("Arguments:")
    logger.info("  --version=???: Sets the version to be used if unspecified")
    logger.info("  --mainClass=???: Sets the main class to run. If unspecified, the manifest will be used to determine the main class to run.")
    logger.info("  --workingDirectory=???: Sets the working directory for the execution environment. If unspecified, the current directory will be used.")
  }
}