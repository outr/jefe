package com.outr.jefe.application

import java.io.File

import com.outr.jefe.launch.{JARLauncher, JMXConfig, Launched, ProcessLauncher}
import com.outr.jefe.resolve._
import reactify.Var

object ProcessApplication {
  def apply(commands: List[String],
            workingDirectory: File = new File("."),
            environment: Map[String, String] = Map.empty): ProcessApplication = {
    new ProcessApplication(new ProcessLauncher(commands, workingDirectory, environment))
  }

  def jar(jars: List[File],
          mainClass: Option[String] = None,
          jvmArgs: List[String] = Nil,
          args: List[String] = Nil,
          jmxConfig: Option[JMXConfig] = None,
          workingDirectory: File = new File("."),
          environment: Map[String, String] = Map.empty): ProcessApplication = {
    new ProcessApplication(new JARLauncher(jars, mainClass, jvmArgs, args, jmxConfig, workingDirectory, environment))
  }

  def artifact(artifacts: List[VersionedArtifact],
               repositories: Repositories = Repositories.default,
               resolver: Resolver = SBTResolver,
               additionalJARs: List[File] = Nil,
               mainClass: Option[String] = None,
               jvmArgs: List[String] = Nil,
               args: List[String] = Nil,
               jmxConfig: Option[JMXConfig] = None,
               workingDirectory: File = new File("."),
               environment: Map[String, String] = Map.empty): ProcessApplication = {
    // Resolve
    val manager = ArtifactManager(repositories, resolver)
    val jars = (artifacts.flatMap { artifact =>
      manager.resolve(artifact)
    } ::: additionalJARs).distinct

    // Create application
    jar(jars, mainClass, jvmArgs, args, jmxConfig, workingDirectory, environment)
  }

  def war(war: File,
          port: Int,
          jvmArgs: List[String] = Nil,
          jmxConfig: Option[JMXConfig] = None,
          workingDirectory: File = new File("."),
          environment: Map[String, String] = Map.empty): ProcessApplication = {
    artifact(
      artifacts = List("org.eclipse.jetty" % "jetty-runner" % "9.4.11.v20180605"),
      repositories = Repositories.default,
      resolver = SBTResolver,
      additionalJARs = Nil,
      mainClass = Some("org.eclipse.jetty.runner.Runner"),
      jvmArgs = jvmArgs,
      args = List("--port", port.toString, war.getCanonicalPath),
      jmxConfig = jmxConfig,
      workingDirectory = workingDirectory,
      environment = environment
    )
  }
}

class ProcessApplication(launcher: ProcessLauncher) extends Application {
  val launched: Var[Option[Launched]] = Var(None)

  override def start(): Unit = if (!isRunning) {
    launched := Some(launcher.launch())
  }

  override def isRunning: Boolean = launched().exists(_.status.isRunning)

  override def restart(force: Boolean): Unit = {
    stop(force)
    start()
  }

  override def stop(force: Boolean): Unit = {
    launched().foreach { l =>
      l.stop(force)
      l.waitForFinished()
    }
    launched := None
  }
}