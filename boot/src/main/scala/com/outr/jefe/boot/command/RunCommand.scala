package com.outr.jefe.boot.command

import com.outr.jefe.application.ProcessApplication
import com.outr.jefe.boot.JefeBoot
import profig.Profig
import com.outr.jefe.resolve._

object RunCommand extends Command {
  private val MavenVersionedRegex = """(.+)[:](.+)[:](.+)""".r
  private val MavenRegex = """(.+)[:](.+)""".r

  override def name: String = "run"
  override def description: String = "Runs an ad-hoc process"

  override def execute(): Unit = {
    Profig("arg2").as[Option[String]] match {
      case Some(applicationInfo) => applicationInfo match {
        case MavenVersionedRegex(groupId, artifactId, version) => {
          val artifact = groupId % artifactId % version
          val mainClass = Profig("mainClass").as[Option[String]].orElse(
            JefeBoot.config(s"$groupId.$artifactId.mainClass").as[Option[String]]
          )
          val application = ProcessApplication.artifact(artifactId, List(artifact), mainClass = mainClass)
          application.start()
          application.waitForFinished()
        }
        case MavenRegex(groupId, artifactId) => {
          val artifact = groupId % artifactId
          val version = Profig("version").as[Option[String]].getOrElse(
            JefeBoot.config(s"$groupId.$artifactId.version").as[Option[String]].getOrElse("latest.release")
          )
          val versioned = artifact % version
          val mainClass = Profig("mainClass").as[Option[String]].orElse(
            JefeBoot.config(s"$groupId.$artifactId.mainClass").as[Option[String]]
          )
          val application = ProcessApplication.artifact(artifactId, List(versioned), mainClass = mainClass)
          application.start()
          application.waitForFinished()
        }
      }
      case None => {
        logger.info("jefe: missing argument")
        help()
      }
    }
  }

  override def help(): Unit = {
    logger.info("Usage: jefe run groupId:artifactId(:version)")
    logger.info("")
    logger.info("You must specify a proper Maven dependency to run.")
    logger.info("If you exclude the version, a Jefe setting of groupId.artifactId.version will be used if specified. Otherwise, the latest release will be used.")
    logger.info("For the version you can also specify 'latest.release' for the latest release or 'latest' for latest integration.")
    logger.info("By default, blocks until the process is terminated")
    logger.info("Arguments:")
    logger.info("  --version=???: Sets the version to be used if unspecified")
    logger.info("  --mainClass=???: Sets the main class to run. If unspecified, the manifest will be used to determine the main class to run.")
  }
}