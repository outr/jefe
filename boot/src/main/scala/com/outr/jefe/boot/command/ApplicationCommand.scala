package com.outr.jefe.boot.command

import java.io.File

import com.outr.jefe.application._
import com.outr.jefe.boot.JefeBoot
import com.outr.jefe.resolve.VersionedArtifact
import org.powerscala.io.IO
import profig.Profig
import com.outr.jefe.resolve._
import io.youi.server.HttpServerListener

trait ApplicationCommand extends Command {
  private val MavenVersionedRegex = """(.+)[:](.+)[:](.+)""".r
  private val MavenRegex = """(.+)[:](.+)""".r

  override def execute(): Unit = loadApplication().foreach(execute)

  protected def allowBackground: Boolean = true

  def execute(application: Application): Unit

  def loadApplication(): Option[Application] = {
    val explicitId = Profig("id").opt[String]
    var mainClass = Profig("mainClass").opt[String]
    val workingDirectory = new File(Profig("workingDirectory").opt[String].getOrElse("."))
    val jvmArgs = loadArgs(List(
      Profig("jvmArgs").opt[String].map(new File(_)),
      Option(new File(workingDirectory, ".jvmopts")),
      Option(new File(".jvmopts"))
    ).flatten: _*)
    val args = loadArgs(List(
      Profig("appArgs").opt[String].map(new File(_)),
      Option(new File(workingDirectory, ".args")),
      Option(new File(".args"))
    ).flatten: _*)
    val background = allowBackground && Profig("background").as[String]("false").toBoolean
    val host = Profig("host").opt[String].getOrElse("localhost")
    val port = Profig("port").opt[Int].getOrElse(8080)
    // TODO: JMX support

    Profig("arg2").opt[String] match {
      case Some(appType) => appType match {
        case "jar" => {
          val jar = Profig("arg3").as[String]
          if (jar.toLowerCase.endsWith(".jar")) {
            val lastSlash = jar.lastIndexOf('/')
            val id = explicitId.getOrElse(jar.substring(lastSlash + 1, jar.lastIndexOf('.')))
            Some(JARApplication(
              id = id,
              jars = List(new File(workingDirectory, jar).getAbsolutePath),
              mainClass = mainClass,
              jvmArgs = jvmArgs,
              args = args,
              workingDirectory = workingDirectory.getAbsolutePath,
              background = background
            ))
          } else {
            fail(s"Expected JAR as next argument, but got: $jar")
          }
        }
        case "war" => {
          val war = Profig("arg3").as[String]
          if (war.toLowerCase.endsWith(".war")) {
            val lastSlash = war.lastIndexOf('/')
            val id = explicitId.getOrElse(war.substring(lastSlash + 1, war.lastIndexOf('.')))
            Some(WARApplication(
              id = id,
              war = new File(workingDirectory, war).getAbsolutePath,
              port = port,
              jvmArgs = jvmArgs,
              workingDirectory = workingDirectory.getAbsolutePath,
              background = background
            ))
          } else {
            fail(s"Expected WAR as next argument, but got: $war")
          }
        }
        case "wars" => {
          Some(MultipleWARApplication(
            id = explicitId.getOrElse(workingDirectory.getName),
            port = port,
            jvmArgs = jvmArgs,
            workingDirectory = workingDirectory.getAbsolutePath,
            background = background
          ))
        }
        case "serve" => {
          val id = explicitId.getOrElse(workingDirectory.getName)
          Some(StaticSiteApplication(id, workingDirectory.getAbsolutePath, Some(HttpServerListener(host, port))))
        }
        case "dependency" => {
          val applicationInfo = Profig("arg3").as[String]
          val artifact: VersionedArtifact = applicationInfo match {
            case MavenVersionedRegex(groupId, artifactId, version) => {
              groupId % artifactId % version
            }
            case MavenRegex(groupId, artifactId) => {
              val version = Profig("version").opt[String].getOrElse(
                JefeBoot.config(s"$groupId.$artifactId.version").opt[String].getOrElse("latest.release")
              )
              groupId % artifactId % version
            }
          }
          mainClass = mainClass.orElse(
            JefeBoot.config(s"${artifact.group}.${artifact.name}.mainClass").opt[String]
          )
          val id = explicitId.getOrElse(artifact.name)
          val repositories = JefeBoot.repositories
          Some(ArtifactApplication(
            id = id,
            artifacts = List(artifact),
            repositories = repositories,
            mainClass = mainClass,
            jvmArgs = jvmArgs,
            args = args,
            workingDirectory = workingDirectory.getAbsolutePath,
            background = background
          ))
        }
        case _ => None
      }
      case None => fail("missing argument")
    }
  }

  private def fail(message: String, showHelp: Boolean = true): Option[Application] = {
    logger.info(s"jefe: $message")
    if (showHelp) help()
    None
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

  def helpArguments: List[(String, String)] = List(
    "id" -> "Optionally sets the identifier for this application (Generated automatically if unset)",
    "version" -> "Sets the version to be used if unspecified",
    "mainClass" -> "Sets the main class to run. If unspecified, the manifest will be used to determine the main class to run.",
    "workingDirectory" -> "Sets the working directory for the execution environment. If unspecified, the current directory will be used.",
    "jvmArgs" -> "Sets the file path to find JVM arguments to be supplied to the process (line separated)",
    "appArgs" -> "Sets the file path to find JVM arguments to be supplied to the process (line separated)",
    "host" -> "Sets the hostname or IP address to bind to for use with serving content. If unspecified, it will use the default (localhost)",
    "port" -> "Sets the port for use with running a WAR or serving content (defaults to 8080 if unspecified)."
  )
  
  override def help(): Unit = {
    logger.info(description)
    logger.info(s"JAR Usage: jefe $name jar path/to/a.jar")
    logger.info(s"WAR Usage: jefe $name war path/to/a.war")
    logger.info(s"WARs Usage: jefe $name wars --workingDirectory=<directory for WAR sub-dirs or contexts>")
    logger.info(s"Serve a path Usage: jefe $name serve")
    logger.info(s"Dependency Usage: jefe $name dependency groupId:artifactId(:version)")
    logger.info("")
    logger.info("Serve Help:")
    logger.info("  Starts a web server serving the content under the specified path")
    logger.info("")
    logger.info("Dependency Help:")
    logger.info(s"  You must specify a proper Maven dependency to $name.")
    logger.info("  If you exclude the version, a Jefe setting of groupId.artifactId.version will be used if specified. Otherwise, the latest release will be used.")
    logger.info("  For the version you can also specify 'latest.release' for the latest release or 'latest' for latest integration.")
    logger.info("")
    logger.info("General Help:")
    logger.info("  JVM Arguments can be provided via '.jvmopts' file found in the current directory or the 'workingDirectory'.")
    logger.info("  Arguments can be provided via '.args' file found in the current directory of the 'workingDirectory'.")
    logger.info("  By default, blocks until the process is terminated")
    logger.info("")
    logger.info("Arguments:")
    helpArguments.foreach {
      case (key, value) => logger.info(s"  --$key=???: $value")
    }
  }
}
