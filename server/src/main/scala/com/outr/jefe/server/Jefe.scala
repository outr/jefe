package com.outr.jefe.server

import java.io.File

import com.outr.jefe.server.config.ProcessApplicationConfig
import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser._
import io.youi.client.{ErrorHandler, HttpClient}
import io.youi.http.{HttpRequest, HttpResponse}
import io.youi.net.URL
import org.powerscala.StringUtil
import org.powerscala.concurrent.Time
import org.powerscala.io._
import profig._
import reactify.Var
import scribe._
import scribe.writer.FileWriter

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object Jefe {
  val password: Var[String] = Var("")

  private var localCommands: Map[String, LocalCommand => Boolean] = Map.empty
  private var remoteCommands: Map[String, LocalCommand => RemoteResponse] = Map.empty

  val started: Long = System.currentTimeMillis()
  val lastModified: Var[Long] = Var(0L)
  val configuration: Var[MainConfiguration] = Var(MainConfiguration())
  val root: Var[File] = Var(new File("."))

  lazy val access: Logger = {
    val logger = new Logger(parentName = None)
    val logPath = Profig("log.access.path").as[Option[String]].map(new File(_)).getOrElse(new File(root(), "logs/access"))
    logger.withHandler(writer = FileWriter.daily("access", directory = logPath.toPath), minimumLevel = Some(Level.Info))
  }

  addLocal("start", start)
  addRemote("update", update)
  addRemote("status", status)
  addRemote("stop", stop)

  def addLocal(command: String, action: LocalCommand => Boolean): Unit = synchronized {
    localCommands += command.toLowerCase -> action
  }

  def addRemote(command: String, action: LocalCommand => RemoteResponse): Unit = synchronized {
    remoteCommands += command.toLowerCase -> action
  }

  def main(args: Array[String]): Unit = {
    Profig.loadDefaults()
    Profig.merge(args)

    root := new File(Profig("path").as[Option[String]].getOrElse("."))
    val configFound = updateConfig()
    Profig("arg1").as[Option[String]] match {
      case Some(command) if configFound | command != "start" => {
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
          |   update - updates the server instance
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
      assert(!Server.isRunning, "Server is not running.")
      val response = Await.result(remote(command), 10.minutes)
      response.messages.foreach { message =>
        scribe.info(message)
      }
      response.success
    } else {
      throw new RuntimeException(s"No command '${command.value}` supported.")
    }
  }

  def run(command: RemoteCommand): RemoteResponse = if (command.password.getOrElse("") == password()) {
    val local = LocalCommand(command.value, Profig.as[MainConfiguration], new File(command.base))
    val action = remoteCommands(local.value)
    action(local)
  } else {
    RemoteResponse(List("Invalid or no password specified!"), success = false)
  }

  /**
    * Called when a command needs to be called remotely.
    */
  def remote(command: LocalCommand): Future[RemoteResponse] = {
    val host = command.configuration.host.getOrElse("localhost")
    val port = command.configuration.port.getOrElse(8080)
    val url = URL(s"http://$host:$port/jefe/remote")
    scribe.info(s"Connecting to $url...")
    val client = new HttpClient
    val future = client.restful[RemoteCommand, RemoteResponse](url, command.toRemote, errorHandler = new ErrorHandler[RemoteResponse] {
      override def apply(request: HttpRequest, response: HttpResponse, throwable: Option[Throwable]): RemoteResponse = {
        scribe.error(response.content.toString)
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
    update(force = true)
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

  def status(command: LocalCommand): RemoteResponse = if (Server.isRunning) {
    var heapCommitted = 0L
    var heapUsed = 0L
    var offheapCommitted = 0L
    var offheapUsed = 0L
    val entries = ProjectManager.instances.flatMap { instance =>
      instance.applications.collect {
        case app: ProcessApplicationConfig => app.processMonitor.map(_.stats()).map { processStats =>
          heapCommitted += processStats.heapUsage.committed
          heapUsed += processStats.heapUsage.used
          offheapCommitted += processStats.nonHeapUsage.committed
          offheapUsed += processStats.nonHeapUsage.used
          List(s"${instance.directory.getName} (PID: ${app.pid.getOrElse(-1)})") ::: processStats.toList.map(l => s"\t$l")
        }
      }.flatten
    }.flatten
    val messages = entries ::: List(
         "",
         s"Total Heap Committed: ${StringUtil.humanReadableByteCount(heapCommitted)}",
         s"Total Heap Used: ${StringUtil.humanReadableByteCount(heapUsed)}",
         s"Total Off-Heap Committed: ${StringUtil.humanReadableByteCount(offheapCommitted)}",
         s"Total Off-Heap Used: ${StringUtil.humanReadableByteCount(offheapUsed)}",
         s"Uptime: ${Time.elapsed(System.currentTimeMillis() - started).toString()}"
    )
    RemoteResponse(messages, success = true)
  } else {
    RemoteResponse(List("Server is not running."), success = false)
  }

  def update(command: LocalCommand): RemoteResponse = if (Server.isRunning) {
    update(force = false)

    RemoteResponse(Nil, success = true)
  } else {
    RemoteResponse(List("Server is not running."), success = false)
  }

  def stop(command: LocalCommand): RemoteResponse = if (Server.isRunning) {
    scribe.info("Received command to shutdown server. Shutting down in 5 seconds...")
    Future {
      Thread.sleep(5000L)
      scribe.info("Shutting down server.")
      Server.stop()
    }
    RemoteResponse(List("Server will shutdown in 5 seconds."), success = true)
  } else {
    RemoteResponse(List("Server not running."), success = false)
  }

  def updateConfig(): Boolean = {
    val jefeConfig = new File(root, "jefe.json")
    if (jefeConfig.exists()) {
      if (lastModified() != jefeConfig.lastModified()) {
        Profig.merge(jefeConfig, ConfigType.Json)
        configuration := Profig.as[MainConfiguration]
        lastModified := jefeConfig.lastModified()
        true
      } else {
        false
      }
    } else {
      if (!Server.isRunning) {
        configuration := Profig.as[MainConfiguration]
      }
      false
    }
  }

  private def update(force: Boolean): Boolean = if (updateConfig() || force) {
    configuration.paths.foreach { path =>
      val directory = new File(root, path)
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
    ProjectManager.stopAllExcept(configuration.paths.map(new File(root, _).getCanonicalPath).toSet)   // Stop removed projects
    true
  } else {
    false
  }
}