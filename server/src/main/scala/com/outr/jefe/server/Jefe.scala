package com.outr.jefe.server

import java.io.File

import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser._
import io.youi.client.HttpClient
import io.youi.net.URL
import org.powerscala.io._
import profig._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object Jefe extends ConfigApplication {
  private var localCommands: Map[String, LocalCommand => Boolean] = Map.empty
  private var remoteCommands: Map[String, LocalCommand => Boolean] = Map.empty

  addLocal("start", start)

  def addLocal(command: String, action: LocalCommand => Boolean): Unit = synchronized {
    localCommands += command.toLowerCase -> action
  }

  def addRemote(command: String, action: LocalCommand => Boolean): Unit = synchronized {
    remoteCommands += command.toLowerCase -> action
  }

  /*def main(args: Array[String]): Unit = {
    if (args.length < 2) {
      println(
        """Usage: jefe <start path> <command> [additional args]*
          | Start Path: Specifies the relative or absolute path to the jefe.json file.
          | Commands:
          |   start - starts the server instance
          |   stop - stops the server instance
          |   status - the current server status
          | Example:
          |   jefe /opt/server/ start
        """.stripMargin)
    } else {
      val baseDirectory = new File(args.head)
      val command = args(1)

      // Load configuration
      val arguments = args.toList.tail.tail
      val configFile = new File(baseDirectory, "jefe.json")
      assert(configFile.isFile, s"Unable to find jefe.json at ${configFile.getAbsolutePath}.")
      val configString = IO.stream(configFile, new StringBuilder).toString
      decode[MainConfiguration](configString) match {
        case Left(error) => throw error
        case Right(configuration) => if (run(LocalCommand(command, arguments, configuration, baseDirectory))) {
          scribe.info(s"Command '$command' completed successfully!")
        } else {
          scribe.error(s"Command '$command' failed!")
        }
      }
    }
  }*/

  override protected def run(): Unit = {
    val root = new File(Config("path").as[Option[String]].getOrElse("."))
    val jefeConfig = new File(root, "jefe.json")
    if (jefeConfig.exists()) Config.merge(jefeConfig)
    val configuration = Config.as[MainConfiguration]
    Config("arg1").as[Option[String]] match {
      case Some(command) if jefeConfig.exists() =>{
        if (run(LocalCommand(command, configuration, root))) {
          scribe.info(s"Command '$command' completed successfully!")
        } else {
          scribe.error(s"Command '$command' failed!")
        }
      }
      case None => println(
        """Usage: jefe <command> [additional args]*
          | Commands:
          |   start - starts the server instance
          |   stop - stops the server instance
          |   status - the current server status
          | Additional Arguments:
          |   --path
          |       Defines the start path where jefe.json can be found. Defaults to the current path.
          | Example:
          |   jefe --path=/opt/server start
        """.stripMargin)
    }
  }

  /**
    * Entry point to run a command.
    */
  def run(command: LocalCommand): Boolean = localCommands.get(command.value) match {
    case Some(action) => action(command)
    case None => if (remoteCommands.contains(command.value)) {
      val response = Await.result(remote(command), 10.minutes)
      response.messages.foreach { message =>
        scribe.info(message)
      }
      response.success
    } else {
      throw new RuntimeException(s"No command '${command.value}` supported.")
    }
  }

  /**
    * Called when a command needs to be called remotely.
    */
  def remote(command: LocalCommand): Future[RemoteResponse] = {
    val host = command.configuration.host.getOrElse("localhost")
    val port = command.configuration.port.getOrElse(8080)
    val url = URL(s"http://$host:$port/jefe/remote")
    val client = new HttpClient
    client.restful[RemoteCommand, RemoteResponse](url, command.toRemote)
  }

  def start(command: LocalCommand): Boolean = {
    val c = command.configuration
    update(command)
    if (c.startServer.getOrElse(true)) {
      ProxyServer.config.clearListeners()
      ProxyServer.config.addHttpListener(c.host.getOrElse("0.0.0.0"), c.port.getOrElse(8080))
      c.ssl.foreach { ssl =>
        ProxyServer.config.addHttpsListener(ssl.host, ssl.port, ssl.password, new File(ssl.keystore))
      }
      ProxyServer.password := c.password.getOrElse("")

      ProxyServer.start()
    }
    CommandSupport.init()

    true
  }

  def update(command: LocalCommand): Boolean = {
    command.configuration.paths.foreach { path =>
      val directory = new File(command.baseDirectory, path)
      var jsonOption: Option[Json] = None
      val configFiles = directory.listFiles().filter(_.getName.endsWith(".jefe.json"))
      configFiles.foreach { f =>
        parse(IO.stream(f, new StringBuilder).toString) match {
          case Left(error) => throw error
          case Right(j) => jsonOption match {
            case Some(json) => jsonOption = Some(json.deepMerge(j))
            case None => jsonOption = Some(j)
          }
        }
      }
      jsonOption match {
        case Some(json) => {
          var jsonString = json.spaces2
          json.hcursor.downField("properties").as[Map[String, String]] match {
            case Left(_) => // No properties found
            case Right(properties) => properties.foreach {
              case (key, value) => jsonString = jsonString.replaceAllLiterally(s"$$$key", value)
            }
          }
          decode[ProjectConfiguration](jsonString) match {
            case Left(error) => throw error
            case Right(config) => ProjectManager(directory) = config
          }
        }
        case None => {
          scribe.warn(s"No JSON found in ${directory.getAbsolutePath}, ignoring.")
        }
      }
    }
    true
  }
}

case class LocalCommand(value: String, configuration: MainConfiguration, baseDirectory: File) {
  def toRemote: RemoteCommand = RemoteCommand(value, configuration.password, baseDirectory.getCanonicalPath)
}

case class RemoteCommand(value: String, password: Option[String], baseDirectory: String)

case class RemoteResponse(messages: List[String], success: Boolean)

case class MainConfiguration(host: Option[String],
                             port: Option[Int],
                             startServer: Option[Boolean],
                             ssl: Option[SSLConfiguration],
                             password: Option[String],
                             paths: List[String])

case class SSLConfiguration(keystore: String, host: String, port: Int, password: String)

case class ProjectConfiguration(proxies: List[ProxyConfiguration],
                                applications: List[ApplicationConfiguration],
                                properties: Map[String, String])

case class ProxyConfiguration(enabled: Boolean, inbound: ProxyInboundConfiguration, outbound: String)

case class ProxyInboundConfiguration(port: Int, domains: List[String])

case class ApplicationConfiguration(`type`: String,
                                    enabled: Option[Boolean],
                                    group: Option[String],
                                    artifact: Option[String],
                                    version: Option[String],
                                    mainClass: Option[String],
                                    scala: Option[Boolean],
                                    scalaVersion: Option[String],
                                    basePath: Option[String],
                                    args: Option[List[String]],
                                    vmArgs: Option[List[String]],
                                    ivyLocal: Option[Boolean],
                                    mavenRepositories: Option[Map[String, String]])