package com.outr.jefe.server

import java.io.File

import com.outr.jefe.repo._
import com.outr.jefe.runner.Repositories
import com.outr.jefe.server.config.{ApplicationConfig, DependencyAppConfig}
import io.youi.http.{HttpConnection, ProxyHandler}
import io.youi.net.URL
import io.youi.server.handler.HttpHandler
import org.powerscala.util.NetUtil

import scala.annotation.tailrec

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
      Some(Server.handler.proxy(new ProxyHandler {
        private val outbound = URL(c.outbound)

        override def proxy(connection: HttpConnection): Option[URL] = {
          val url = connection.request.url
          if (c.inbound.domains.exists(url.host.matches) && c.inbound.port == url.port) {
            val out = outbound.withPath(url.path.toString())
            Jefe.access.info(s"Proxying for ${directory.getPath} - From: $url, To: $out")
            Some(out)
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
    scribe.info(s"Starting ${directory.getName}...")
    applications.foreach(_.start())
    proxies.foreach(proxy => Server.handlers += proxy)
  }
  def stop(): Unit = {
    scribe.info(s"Stopping ${directory.getName}...")
    proxies.foreach(proxy => Server.handlers -= proxy)
    applications.foreach(_.stop())
  }
}