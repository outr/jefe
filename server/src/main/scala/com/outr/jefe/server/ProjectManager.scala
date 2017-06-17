package com.outr.jefe.server

import java.io.File

import com.outr.jefe.runner.Repositories
import com.outr.jefe.repo._
import com.outr.jefe.server.config.{ApplicationConfig, DependencyAppConfig}
import io.youi.http.{HttpConnection, ProxyHandler}
import io.youi.net.URL
import io.youi.server.handler.HttpHandler
import org.powerscala.util.NetUtil

import scala.annotation.tailrec

object ProjectManager {
  private var instances = Map.empty[String, ProjectInstance]

  def update(directory: File, configuration: ProjectConfiguration): Unit = synchronized {
    val path = directory.getCanonicalPath
    val currentOption = instances.get(path)
    if (currentOption.map(_.configuration).contains(configuration)) {
      scribe.info(s"$path has not changed")
    } else {
      if (currentOption.nonEmpty) {
        scribe.info(s"$path configuration has changed. Reloading...")
        currentOption.foreach(_.stop())
      } else {
        scribe.info(s"$path is loading...")
      }
      val instance = new ProjectInstance(directory, configuration)
      instances += path -> instance
      instance.start()
    }
  }
}

class ProjectInstance(val directory: File, val configuration: ProjectConfiguration) {
  private val applications: List[ApplicationConfig] = configuration.applications.map { c =>
    def getOrError[T](name: String, option: Option[T]): T = option.getOrElse(throw new RuntimeException(s"No '$name' provided in configuration for ${c.`type`} for ${directory.getName}."))

    val enabled = c.enabled.getOrElse(true)
    def group = getOrError("group", c.group)
    def artifact = getOrError("artifact", c.artifact)
    def version = getOrError("version", c.version)
    def mainClass = getOrError("mainClass", c.mainClass)
    val scala = c.scala.getOrElse(true)
    val scalaVersion = if (scala) Some(c.scalaVersion.getOrElse(CurrentScalaVersion)) else None
    val baseDirectory = c.basePath.map(p => new File(directory, p)).getOrElse(directory)
    val args = c.args.getOrElse(Nil)
    val vmArgs = c.vmArgs.getOrElse(Nil)
    val mavenRepositories = c.mavenRepositories.getOrElse(Map.empty).map {
      case (name, url) => MavenRepository(name, url)
    }.toList match {
      case Nil => List(Maven.Repo1, Sonatype.Releases, Sonatype.Snapshots)
      case list => list
    }
    val repositoriesList: List[Repository] = List(
      if (c.ivyLocal.getOrElse(true)) List(Ivy2.Local) else Nil,
      mavenRepositories
    ).flatten
    val repositories = Repositories(repositoriesList)
    val jmxPort = ProjectInstance.nextJMXPort()
    val additionalArgs = List(
      "-Dcom.sun.management.jmxremote=true",
      "-Djava.rmi.server.hostname=127.0.0.1",
      s"-Dcom.sun.management.jmxremote.port=$jmxPort",
      "-Dcom.sun.management.jmxremote.authenticate=false",
      "-Dcom.sun.management.jmxremote.ssl=false",
      "-Dcom.sun.management.jmxremote.local.only=false"
    )
    c.`type` match {
      case "dependency" => new DependencyAppConfig(
        enabled = enabled,
        workingDirectory = baseDirectory,
        group = group,
        artifact = artifact,
        version = version,
        mainClass = mainClass,
        args = args,
        jmxPort = jmxPort,
        vmArgs = vmArgs ::: additionalArgs,
        repositories = repositories,
        scalaVersion = scalaVersion
      )
      // TODO: support static app, jar app, and war app
      case t => throw new UnsupportedOperationException(s"Unsupported application type: '$t'.")
    }
  }
  private val proxies: List[HttpHandler] = configuration.proxies.flatMap { c =>
    if (c.enabled) {
      Some(ProxyServer.handler.proxy(new ProxyHandler {
        private val outbound = URL(c.outbound)

        override def proxy(connection: HttpConnection): Option[URL] = {
          val url = connection.request.url
          if (c.inbound.domains.exists(url.host.matches) && c.inbound.port == url.port) {
            Some(outbound)
          } else {
            None
          }
        }
      }))
    } else {
        None
    }
  }

  def start(): Unit = {
    applications.foreach(_.start())
    proxies.foreach(proxy => ProxyServer.handlers += proxy)
  }
  def stop(): Unit = {
    proxies.foreach(proxy => ProxyServer.handlers -= proxy)
    applications.foreach(_.stop())
  }
}

object ProjectInstance {
  private var counter = 10000

  def nextJMXPort(): Int = synchronized(nextJMXPortRecursive())

  @tailrec
  private def nextJMXPortRecursive(): Int = {
    val port = counter
    counter += 1
    if (NetUtil.isTCPPortFree(port)) {
      port
    } else {
      nextJMXPortRecursive()
    }
  }
}