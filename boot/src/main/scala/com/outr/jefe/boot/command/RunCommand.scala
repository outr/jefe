package com.outr.jefe.boot.command

import java.io.File

import com.outr.jefe.application.{Application, ProcessApplication}
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
    var mainClass = Profig("mainClass").as[Option[String]]
    val workingDirectory = new File(Profig("workingDirectory").as[Option[String]].getOrElse("."))
    val jvmArgs = loadArgs(List(
      Profig("jvmArgs").as[Option[String]].map(new File(_)),
      Option(new File(workingDirectory, ".jvmopts")),
      Option(new File(".jvmopts"))
    ).flatten: _*)
    val args = loadArgs(List(
      Profig("args").as[Option[String]].map(new File(_)),
      Option(new File(workingDirectory, ".args")),
      Option(new File(".args"))
    ).flatten: _*)

    val applicationOption: Option[Application] = Profig("arg2").as[Option[String]] match {
      case Some(jar) if jar.toLowerCase.endsWith(".jar") => {
        val lastSlash = jar.lastIndexOf('/')
        val id = jar.substring(lastSlash + 1, jar.lastIndexOf('.'))
        Some(ProcessApplication.jar(
          id = id,
          jars = List(new File(workingDirectory, jar)),
          mainClass = mainClass,
          jvmArgs = jvmArgs,
          args = args,
          workingDirectory = workingDirectory
        ))
      }
      case Some(war) if war.toLowerCase.endsWith(".war") => {
        val lastSlash = war.lastIndexOf('/')
        val id = war.substring(lastSlash + 1, war.lastIndexOf('.'))
        val port = Profig("port").as[Option[Int]].getOrElse(8080)
        Some(ProcessApplication.war(
          id = id,
          war = new File(workingDirectory, war),
          port = port,
          jvmArgs = jvmArgs,
          workingDirectory = workingDirectory
        ))
      }
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
        mainClass = mainClass.orElse(
          JefeBoot.config(s"${artifact.group}.${artifact.name}.mainClass").as[Option[String]]
        )
        val repositories = JefeBoot.repositories
        Some(ProcessApplication.artifact(
          id = artifact.name,
          artifacts = List(artifact),
          repositories = repositories,
          mainClass = mainClass,
          jvmArgs = jvmArgs,
          args = args,
          workingDirectory = workingDirectory
        ))
      }
      case None => {
        logger.info("jefe: missing argument")
        help()
        None
      }
    }
    applicationOption match {
      case Some(application) => application.start()
      case _ => // Nothing
    }
  }

  def loadArgs(files: File*): List[String] = {
    files.flatMap { file =>
      if (file.exists()) {
        IO.stream(file, new StringBuilder).toString.split("\n").map(_.trim).filter(_.nonEmpty).toList
      } else {
        Nil
      }
    }.toList.distinct
  }

  override def help(): Unit = {
    logger.info("JAR Usage: jefe run path/to/a.jar")
    logger.info("WAR Usage: jefe run path/to/a.war")
    logger.info("Dependency Usage: jefe run groupId:artifactId(:version)")
    logger.info("")
    logger.info("Dependency Help:")
    logger.info("  You must specify a proper Maven dependency to run.")
    logger.info("  If you exclude the version, a Jefe setting of groupId.artifactId.version will be used if specified. Otherwise, the latest release will be used.")
    logger.info("  For the version you can also specify 'latest.release' for the latest release or 'latest' for latest integration.")
    logger.info("")
    logger.info("General Help:")
    logger.info("  JVM Arguments can be provided via '.jvmopts' file found in the current directory or the 'workingDirectory'.")
    logger.info("  Arguments can be provided via '.args' file found in the current directory of the 'workingDirectory'.")
    logger.info("  By default, blocks until the process is terminated")
    logger.info("")
    logger.info("Arguments:")
    logger.info("  --version=???: Sets the version to be used if unspecified")
    logger.info("  --mainClass=???: Sets the main class to run. If unspecified, the manifest will be used to determine the main class to run.")
    logger.info("  --workingDirectory=???: Sets the working directory for the execution environment. If unspecified, the current directory will be used.")
    logger.info("  --jvmArgs=???: Sets the file path to find JVM arguments to be supplied to the process (line separated)")
    logger.info("  --args=???: Sets the file path to find JVM arguments to be supplied to the process (line separated)")
    logger.info("  --port=???: Sets the port for use with running a WAR (defaults to 8080 if unspecified)")
  }
}