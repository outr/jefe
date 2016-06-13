package com.outr.jefe.server

import java.io.File
import java.net.{URI, URL}

import com.outr.jefe.launch.LauncherInstance
import com.outr.jefe.runner.{Arguments, Configuration, Runner}
import pl.metastack.metarx.{Buffer, Sub}

import scala.xml.{Elem, NodeSeq, XML}
import com.outr.jefe.repo._

object JefeServer {
  // TODO: pick up new directories created in base-dir, use jefe.config files (generate if not defined)
  // TODO: support starting JAR
  // TODO: support starting WAR
  // TODO: support starting Docker instance
  // TODO: inject communication utility into apps to control and access runtime info
  // TODO: access process ids
  // TODO: support console commands (help, enable, disable, status, apps, shutdown)

  val configurations: Buffer[AppConfiguration] = Buffer()

  def main(temp: Array[String]): Unit = {
    val args = Array("start", "directory=../servertest")
    if (args.isEmpty) {
      println(
        """Usage: jefe <command> (options)
          | Commands:
          |   start - starts the server instance
          |     password - the password required for interaction. defaults to "".
          |     host - the host to bind to. defaults to localhost.
          |     port - the port to bind to. defaults to 8080.
          |     background - whether this should run in the background or foreground. defaults to false.
          |     directory - the directory to manage. defaults to current directory.
          |   stop - stops the server instance
          |     password - the password required for interaction. defaults to "".
          |     host - the host the server is currently running on. defaults to localhost.
          |     port - the port the server is currently running on. defaults to localhost.
          |   status - the current server status
          |     password - the password required for interaction. defaults to "".
          |     host - the host the server is currently running on. defaults to localhost.
          |     port - the port the server is currently running on. defaults to localhost.
          |     app - the application to get the status of. if not supplied gives general outline of all apps.
          | Example:
          |   jefe start password="secure" host=127.0.0.1 port=80 background=true directory=/opt/applications
        """.stripMargin)
      System.exit(1)
    }
    val action = args.head
    val arguments = new Arguments(args.tail)
    val password = arguments.takeOrElse("password", "")
    val host = arguments.takeOrElse("host", "localhost")
    val port = arguments.takeOrElse("port", "8080").toInt
    val background = arguments.takeOrElse("background", "false").toBoolean

    val directory = new File(arguments.takeOrElse("directory", "."))
    val app = arguments.takeOrElse("app", "")

    action match {
      case "start" => {
        // TODO: support background

        ProxyServer.host := host
        ProxyServer.port := port
        ProxyServer.password := password

        monitor(directory)

//        val config = loadConfiguration(getClass.getClassLoader.getResource("template.xml"))
//        config.application.foreach(_.start())
//        configurations += config
//        ProxyServer.start()
      }
      case "stop" => {

      }
      case "status" => {

      }
    }
  }

  def monitor(directory: File): Unit = {

  }

  def loadConfiguration(config: URL): AppConfiguration = {
    val xml = XML.load(config)
    val proxy = (xml \ "proxy").headOption.map { p =>
      val enabled = (p \ "enabled").bool
      val inbound = (p \ "inbound").head.flatMap(_.child.collect {
        case e: Elem => e.label match {
          case "domain" => InboundDomain(e.text)
          case label => throw new RuntimeException(s"Unsupported inbound type: $label.")
        }
      }).toList
      val outbound = new URI((p \ "outbound").text)
      ProxyConfig(enabled, inbound, outbound)
    }
    val app = (xml \ "application").headOption.map { a =>
      val enabled = (a \ "enabled").bool
      val mainClass = (a \ "mainClass").string
      val args = (a \ "arg").map(_.text)
      (a \ "type").string match {
        case "dependency" => {
          val group = (a \ "group").string
          val artifact = (a \ "artifact").string
          val version = (a \ "version").string
          DependencyAppConfig(enabled, group, artifact, version, mainClass, args)
        }
      }
    }
    AppConfiguration(proxy, app)
  }

  implicit class ExtraNode(n: NodeSeq) {
    def bool = n.headOption.exists(_.text.toBoolean)
    def string = n.text
  }
}

case class AppConfiguration(proxy: Option[ProxyConfig],
                            application: Option[ApplicationConfig])

case class ProxyConfig(enabled: Boolean = false,
                       inbound: List[Inbound],
                       outbound: URI)

trait Inbound

case class InboundDomain(domain: String) extends Inbound

trait ApplicationConfig {
  def enabled: Boolean
  def mainClass: String
  def args: Seq[String]

  def start(): Unit
  def stop(): Unit
}

case class DependencyAppConfig(enabled: Boolean,
                               group: String,
                               artifact: String,
                               version: String,
                               mainClass: String,
                               args: Seq[String]) extends ApplicationConfig {
  private var instance: Option[LauncherInstance] = None

  override def start(): Unit = synchronized {
    stop()

    val dependency = group %% artifact % version
    val config = Configuration(dependency, mainClass, args.toArray, newProcess = true)
    val li = Runner.run(config)
    instance = Some(li)
    li.start()
  }

  override def stop(): Unit = synchronized {
    instance match {
      case Some(li) => li.stop()
      case None => // No instance
    }
    instance = None
  }
}