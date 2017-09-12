package com.outr.jefe.server

import java.io.File

import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser._
import io.youi.client.{ErrorHandler, HttpClient}
import io.youi.http.{HttpRequest, HttpResponse}
import io.youi.net.URL
import org.powerscala.io._
import profig._
import reactify.Var
import scribe.formatter.Formatter
import scribe._
import scribe.writer.FileWriter

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object Jefe extends ConfigApplication {
  val password: Var[String] = Var("")

  lazy val access: Logger = {
    val logger = new Logger(parentName = None)
    val logPath = Config("log.access.path").as[Option[String]].map(new File(_)).getOrElse(new File("logs/access"))
    logger.addHandler(LogHandler(Level.Info, Formatter.default, FileWriter.daily("access", logPath)))
    logger
  }

  private var localCommands: Map[String, LocalCommand => Boolean] = Map.empty
  private var remoteCommands: Map[String, LocalCommand => Boolean] = Map.empty

  val configuration: Var[MainConfiguration] = Var(MainConfiguration())
  val root: Var[File] = Var(new File("."))

  addLocal("start", start)
  addRemote("stop", stop)

  def addLocal(command: String, action: LocalCommand => Boolean): Unit = synchronized {
    localCommands += command.toLowerCase -> action
  }

  def addRemote(command: String, action: LocalCommand => Boolean): Unit = synchronized {
    remoteCommands += command.toLowerCase -> action
  }

  override def main(args: Array[String]): Unit = {
    scribe.info(s"Args (${args.length}): ${args.mkString(", ")}")
    start(args)
  }

  override protected def run(): Unit = {
    root := new File(Config("path").as[Option[String]].getOrElse("."))
    val jefeConfig = new File(root, "jefe.json")
    if (jefeConfig.exists()) Config.merge(jefeConfig)
    configuration := Config.as[MainConfiguration]
    Config("arg1").as[Option[String]] match {
      case Some(command) if jefeConfig.exists() =>{
        if (run(LocalCommand(command, configuration, root))) {
          scribe.info(s"Command '$command' completed successfully!")
        } else {
          scribe.error(s"Command '$command' failed!")
        }
      }
      case Some(_) => println(s"No jefe.json file found at ${root.getAbsolutePath}")
      case _ => println(
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
      if (Server.isRunning) {
        val action = remoteCommands(command.value)
        action(command)
      } else {
        val response = Await.result(remote(command), 10.minutes)
        response.messages.foreach { message =>
          scribe.info(message)
        }
        response.success
      }
    } else {
      throw new RuntimeException(s"No command '${command.value}` supported.")
    }
  }

  def localize(command: RemoteCommand): LocalCommand = {
    LocalCommand(command.value, Config.as[MainConfiguration], new File(command.base))
  }

  /**
    * Called when a command needs to be called remotely.
    */
  def remote(command: LocalCommand): Future[RemoteResponse] = {
    val host = command.configuration.host.getOrElse("localhost")
    val port = command.configuration.port.getOrElse(8080)
    val url = URL(s"http://$host:$port/jefe/remote")
    val client = new HttpClient
    val future = client.restful[RemoteCommand, RemoteResponse](url, command.toRemote, errorHandler = new ErrorHandler[RemoteResponse] {
      override def apply(request: HttpRequest, response: HttpResponse, throwable: Option[Throwable]): RemoteResponse = {
        scribe.error(response.content)
        throw throwable.getOrElse(new RuntimeException("Error while processing response!"))
      }
    })
    future.onComplete { _ =>
      client.dispose()
    }
    future
  }

  def start(command: LocalCommand): Boolean = {
    val c = command.configuration
    update(command)
    if (c.startServer.getOrElse(true)) {
      Server.config.clearListeners()
      Server.config.addHttpListener(c.host.getOrElse("0.0.0.0"), c.port.getOrElse(8080))
      c.ssl.foreach { ssl =>
        Server.config.addHttpsListener(ssl.host, ssl.port, ssl.password, new File(ssl.keystore))
      }
      password := c.password.getOrElse("")

      Server.start()
    }

    true
  }

  def stop(command: LocalCommand): Boolean = if (Server.isRunning) {
    scribe.info("Received command to shutdown server. Shutting down in 5 seconds...")
    Future {
      Thread.sleep(5000L)
      scribe.info("Shutting down server.")
      Server.stop()
    }
    true
  } else {
    false
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

case class RemoteCommand(value: String, password: Option[String], base: String)

case class RemoteResponse(messages: List[String], success: Boolean)

case class MainConfiguration(host: Option[String] = None,
                             port: Option[Int] = None,
                             startServer: Option[Boolean] = None,
                             ssl: Option[SSLConfiguration] = None,
                             password: Option[String] = None,
                             paths: List[String] = Nil)

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