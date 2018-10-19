package com.outr.jefe.application

import java.io.File

import com.outr.jefe.launch.{JARLauncher, Launched, ProcessLauncher, ProcessStatus}
import com.outr.jefe.launch.jmx.{JMXConfig, ProcessStats}
import com.outr.jefe.resolve.{ArtifactManager, Repositories, Resolver, VersionedArtifact}
import reactify.Var
import scribe.Logger
import com.outr.jefe.resolve._
import io.youi.server.{HttpServerListener, HttpsServerListener, Server}

sealed trait Application {
  def id: String
  def enabled: Boolean
  def start(): Unit
  def isRunning: Boolean
  def restart(force: Boolean): Unit
  def stats(): Option[ProcessStats]
  def stop(force: Boolean): Unit
}

sealed trait ApplicationProcess extends Application {
  protected def launcher: ProcessLauncher

  lazy val launched: Var[Option[Launched]] = Var(None)

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

case class ProcessApplication(id: String,
                              commands: List[String],
                              workingDirectory: String = ".",
                              environment: Map[String, String] = Map.empty,
                              loggerId: Long = Logger.rootId,
                              background: Boolean = false,
                              enabled: Boolean = true) extends ApplicationProcess {
  override protected def launcher: ProcessLauncher = new ProcessLauncher(commands, new File(workingDirectory), environment, loggerId, background)
}

case class JARApplication(id: String,
                          jars: List[String],
                          mainClass: Option[String] = None,
                          jvmArgs: List[String] = Nil,
                          args: List[String] = Nil,
                          jmxConfig: Option[JMXConfig] = None,
                          workingDirectory: String = ".",
                          environment: Map[String, String] = Map.empty,
                          loggerId: Long = Logger.rootId,
                          background: Boolean = false,
                          enabled: Boolean = true) extends ApplicationProcess {
  override protected def launcher: ProcessLauncher = {
    val files = jars.map(path => new File(path))
    new JARLauncher(files, mainClass, jvmArgs, args, jmxConfig, new File(workingDirectory), environment, loggerId, background)
  }
}

case class ArtifactApplication(id: String,
                               artifacts: List[VersionedArtifact],
                               repositories: Repositories = Repositories.default,
                               resolver: String = "sbt",
                               additionalJARs: List[String] = Nil,
                               mainClass: Option[String] = None,
                               jvmArgs: List[String] = Nil,
                               args: List[String] = Nil,
                               jmxConfig: Option[JMXConfig] = None,
                               workingDirectory: String = ".",
                               environment: Map[String, String] = Map.empty,
                               loggerId: Long = Logger.rootId,
                               background: Boolean = false,
                               enabled: Boolean = true) extends ApplicationProcess {
  override protected def launcher: ProcessLauncher = {
    // Resolve
    val resolverInstance = if (resolver.toLowerCase == "coursier") {
      CoursierResolver
    } else {
      SBTResolver
    }
    val manager = ArtifactManager(repositories, resolverInstance)
    val jars = (artifacts.flatMap { artifact =>
      manager.resolve(artifact)
    } ::: additionalJARs.map(new File(_))).distinct

    // Create application
    new JARLauncher(jars, mainClass, jvmArgs, args, jmxConfig, new File(workingDirectory), environment, loggerId, background)
  }
}

case class WARApplication(id: String,
                          war: String,
                          port: Int,
                          jvmArgs: List[String] = Nil,
                          jmxConfig: Option[JMXConfig] = None,
                          workingDirectory: String = ".",
                          environment: Map[String, String] = Map.empty,
                          loggerId: Long = Logger.rootId,
                          background: Boolean = false,
                          enabled: Boolean = true) extends ApplicationProcess {
  override protected def launcher: ProcessLauncher = {
    // Resolve
    val manager = ArtifactManager(Repositories.default, Resolver.default)
    val artifacts = List("org.eclipse.jetty" % "jetty-runner" % "9.4.11.v20180605")
    val jars = artifacts.flatMap { artifact =>
      manager.resolve(artifact)
    }

    val mainClass = Some("org.eclipse.jetty.runner.Runner")
    val args = List("--port", port.toString, war)

    // Create application
    new JARLauncher(jars, mainClass, jvmArgs, args, jmxConfig, new File(workingDirectory), environment, loggerId, background)
  }
}

case class StaticSiteApplication(id: String,
                                 directory: String,
                                 http: Option[HttpServerListener] = None,
                                 https: Option[HttpsServerListener] = None,
                                 enabled: Boolean = true) extends Application {
  assert(http.nonEmpty || https.nonEmpty, "At least one server must be configured!")

  object server extends Server {
    config.listeners := List(http, https).flatten
    handler.file(new File(directory))
  }

  override def start(): Unit = {
    scribe.info(s"Serving $directory...")
    server.start()
  }

  override def isRunning: Boolean = server.isRunning

  override def restart(force: Boolean): Unit = server.restart()

  override def stats(): Option[ProcessStats] = None

  override def stop(force: Boolean): Unit = server.stop()
}