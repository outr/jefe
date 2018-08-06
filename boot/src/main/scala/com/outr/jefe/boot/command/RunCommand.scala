package com.outr.jefe.boot.command

import com.outr.jefe.application.ProcessApplication
import profig.Profig
import com.outr.jefe.resolve._

object RunCommand extends Command {
  private val MavenRegex = """(.+)[:](.+)[:](.+)""".r

  override def name: String = "run"
  override def description: String = "Runs an ad-hoc process"

  override def execute(): Unit = {
    Profig("arg2").as[Option[String]] match {
      case Some(applicationInfo) => applicationInfo match {
        case MavenRegex(groupId, artifactId, version) => {
          val artifact = groupId % artifactId % version
          val mainClass = Profig("mainClass").as[Option[String]]
          val application = ProcessApplication.artifact(artifactId, List(artifact), mainClass = mainClass)
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
    logger.info("Usage: jefe run groupId:artifactId:version")
    logger.info("")
    logger.info("You must specify a proper Maven dependency to run.")
    logger.info("By default, blocks until the process is terminated")
  }
}