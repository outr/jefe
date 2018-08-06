package com.outr.jefe.application

import java.io.File

import com.outr.jefe.launch._
import com.outr.jefe.launch.jmx.{JMXConfig, ProcessStats}
import com.outr.jefe.resolve._
import reactify.Var

object ProcessApplication {
  def apply(id: String,
            commands: List[String],
            workingDirectory: File = new File("."),
            environment: Map[String, String] = Map.empty): ProcessApplication = {
    new ProcessApplication(id, new ProcessLauncher(commands, workingDirectory, environment))
  }

  def jar(id: String,
          jars: List[File],
          mainClass: Option[String] = None,
          jvmArgs: List[String] = Nil,
          args: List[String] = Nil,
          jmxConfig: Option[JMXConfig] = None,
          workingDirectory: File = new File("."),
          environment: Map[String, String] = Map.empty): ProcessApplication = {
    new ProcessApplication(id, new JARLauncher(jars, mainClass, jvmArgs, args, jmxConfig, workingDirectory, environment))
  }

  def artifact(id: String,
               artifacts: List[VersionedArtifact],
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
    jar(id, jars, mainClass, jvmArgs, args, jmxConfig, workingDirectory, environment)
  }

  def war(id: String,
          war: File,
          port: Int,
          jvmArgs: List[String] = Nil,
          jmxConfig: Option[JMXConfig] = None,
          workingDirectory: File = new File("."),
          environment: Map[String, String] = Map.empty): ProcessApplication = {
    artifact(
      id = id,
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

class ProcessApplication(val id: String, launcher: ProcessLauncher) extends Application {
  val launched: Var[Option[Launched]] = Var(None)

  override def start(): Unit = if (!isRunning) {
    launched := Some(launcher.launch())
  }

  override def isRunning: Boolean = launched().exists(_.status.isRunning)

  override def restart(force: Boolean): Unit = {
    stop(force)
    start()
  }

  def waitForFinished(): ProcessStatus = launched().map(_.waitForFinished()).getOrElse(throw new RuntimeException("Not started!"))

  override def stats(): Option[ProcessStats] = launched().flatMap(_.stats())

  override def stop(force: Boolean): Unit = {
    launched().foreach { l =>
      l.stop(force)
      l.waitForFinished()
    }
    launched := None
  }
}