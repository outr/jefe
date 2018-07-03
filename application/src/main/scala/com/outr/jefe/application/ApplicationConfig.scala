package com.outr.jefe.application

import java.io.File

import com.outr.jefe.launch.JMXConfig
import com.outr.jefe.resolve.{CoursierResolver, Repositories, SBTResolver, VersionedArtifact}
import io.youi.server.{HttpServerListener, HttpsServerListener}

sealed trait ApplicationConfig {
  def id: String

  def create(): Application
}

case class ProcessConfig(id: String,
                         commands: List[String],
                         workingDirectory: String,
                         environment: Map[String, String] = Map.empty) extends ApplicationConfig {
  override def create(): Application = ProcessApplication(id, commands, new File(workingDirectory), environment)
}

case class JARConfig(id: String,
                     jars: List[String],
                     mainClass: Option[String] = None,
                     jvmArgs: List[String] = Nil,
                     args: List[String] = Nil,
                     jmxConfig: Option[JMXConfig] = None,
                     workingDirectory: String,
                     environment: Map[String, String]) extends ApplicationConfig {
  override def create(): Application = {
    val jarFiles = jars.map(new File(_))
    ProcessApplication.jar(id, jarFiles, mainClass, jvmArgs, args, jmxConfig, new File(workingDirectory), environment)
  }
}

case class ArtifactConfig(id: String,
                          artifacts: List[VersionedArtifact],
                          repositories: Repositories,
                          useCoursier: Boolean,
                          additionalJARs: List[String] = Nil,
                          mainClass: Option[String] = None,
                          jvmArgs: List[String] = Nil,
                          args: List[String] = Nil,
                          jmxConfig: Option[JMXConfig] = None,
                          workingDirectory: String,
                          environment: Map[String, String]) extends ApplicationConfig {
  override def create(): Application = {
    val additionalJARFiles = additionalJARs.map(new File(_))
    ProcessApplication.artifact(
      id = id,
      artifacts = artifacts,
      repositories = repositories,
      resolver = if (useCoursier) CoursierResolver else SBTResolver,
      additionalJARs = additionalJARFiles,
      mainClass = mainClass,
      jvmArgs = jvmArgs,
      args = args,
      jmxConfig = jmxConfig,
      workingDirectory = new File(workingDirectory),
      environment = environment
    )
  }
}

case class WARConfig(id: String,
                     war: String,
                     port: Int,
                     jvmArgs: List[String] = Nil,
                     jmxConfig: Option[JMXConfig] = None,
                     workingDirectory: String,
                     environment: Map[String, String]) extends ApplicationConfig {
  override def create(): Application = {
    ProcessApplication.war(id, new File(war), port, jvmArgs, jmxConfig, new File(workingDirectory), environment)
  }
}

case class StaticSiteConfig(id: String,
                            directory: String,
                            http: Option[HttpServerListener] = None,
                            https: Option[HttpsServerListener] = None) extends ApplicationConfig {
  override def create(): Application = new StaticSiteApplication(id, new File(directory), http, https)
}