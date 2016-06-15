package com.outr.jefe.server

import java.io.File
import java.net.{URI, URL, URLEncoder}

import com.outr.jefe.launch.{Launcher, LauncherInstance}
import com.outr.jefe.runner.{Arguments, Configuration, Runner}
import pl.metastack.metarx.Buffer

import scala.xml.{Elem, NodeSeq, XML}
import com.outr.jefe.repo._
import com.outr.scribe.writer.FileWriter
import com.outr.scribe.{LogHandler, Logger, Logging}
import org.powerscala.io._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object JefeServer extends Logging {
  // TODO: support starting Docker instance

  val configurations: Buffer[AppConfiguration] = Buffer()
  var directory: File = _

  def main(args: Array[String]): Unit = {
//    val args = Array("start", "directory=../servertest")
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

    directory = new File(arguments.takeOrElse("directory", "."))
    val app = arguments.takeOrElse("app", "")

    Logger.Root.addHandler(LogHandler(writer = FileWriter.Daily("jefe", new File(directory, "logs"))))

    action match {
      case "start" => {
        // TODO: support background

        ProxyServer.host := host
        ProxyServer.port := port
        ProxyServer.password := password

        updateDirectories()

        ProxyServer.start()

        CommandSupport.init()
      }
      case "stop" => send(host, port, password, "stop", Map.empty)
      case "status" => send(host, port, password, "status", Map("app" -> app))
      case "list" => send(host, port, password, "list", Map.empty)
      case "update" => send(host, port, password, "update", Map.empty)
    }
  }

  def send(host: String, port: Int, password: String, command: String, args: Map[String, String]): Unit = {
    val argsString = (args + ("password" -> password)).map(t => s"${t._1}=${URLEncoder.encode(t._2, "UTF-8")}").mkString("&")
    val url = new URL(s"http://$host:$port/jefe/$command?${argsString}")
    val response = IO.stream(url.openStream(), new StringBuilder).toString
    println(response)
  }

  def list(): String = {
    val heading = s"Listing ${configurations.get.size} configuration(s):"
    val items = configurations.get.map(appConfig => s"${appConfig.name}: ${appConfig.proxy} / ${appConfig.application}").mkString("\n")
    s"$heading\n$items"
  }

  def updateDirectories(): Unit = {
    logger.info("Checking for updates...")
    var dirNames = Set.empty[String]

    // Look at existing directories
    directory.listFiles().filter(_.isDirectory).foreach { dir =>
      if (!dir.isHidden && !dir.getName.startsWith(".") && dir.getName != "logs") {
        dirNames += dir.getName
        updateDirectory(dir)
      }
    }

    // Remove old directories
    configurations.get.foreach { appConfig =>
      if (!dirNames.contains(appConfig.name)) {
        updateDirectory(new File(directory, appConfig.name))
      }
    }
  }

  def updateDirectory(directory: File): Unit = synchronized {
    val name = directory.getName
    if (directory.exists()) {
      val config = new File(directory, "jefe.config.xml")
      if (!config.exists()) {
        IO.stream(getClass.getClassLoader.getResource("template.xml"), config)
      }
      val appConfig = JefeServer.loadConfiguration(directory)
      val oldConfig = configurations.get.find(_.name == name)
      if (oldConfig.isEmpty || oldConfig.get.lastModified != appConfig.lastModified) {
        logger.info(s"Configuration change for ${directory.getName}, reloading...")
        oldConfig.foreach(_.application.foreach(_.stop())) // Shutdown the app if it's running
        appConfig.application.foreach { app => // Start the app from the new config
          if (app.enabled) {
            app.start()
          }
        }
        oldConfig match {
          case Some(c) => JefeServer.configurations.replace(c, appConfig)
          case None => JefeServer.configurations += appConfig
        }
      }
    } else {
      configurations.get.find(_.name == name).foreach(_.application.foreach(_.stop()))
    }
  }

  def loadConfiguration(directory: File): AppConfiguration = {
    val name = directory.getName
    val configFile = new File(directory, "jefe.config.xml")
    val lastModified = configFile.lastModified()
    val config = configFile.toURI.toURL
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
        case "jar" => {
          val jar = new File(directory, (a \ "jar").string)
          if (!jar.exists()) {
            throw new RuntimeException(s"JAR doesn't exist: ${jar.getAbsolutePath}")
          }
          new JARAppConfig(enabled, jar, mainClass, args)
        }
        case "war" => {
          val war = new File(directory, (a \ "war").string)
          if (!war.exists()) {
            throw new RuntimeException(s"WAR doesn't exist: ${war.getAbsolutePath}")
          }
          val port = (a \ "port").int
          new WARAppConfig(enabled, war, port)
        }
        case "dependency" => {
          val group = (a \ "group").string
          val artifact = (a \ "artifact").string
          val version = (a \ "version").string
          new DependencyAppConfig(enabled, directory, group, artifact, version, mainClass, args)
        }
      }
    }
    AppConfiguration(name, lastModified, proxy, app)
  }

  def shutdown(): Unit = Future({
    Thread.sleep(1000)      // Wait for one second
    configurations.foreach(_.application.foreach(_.stop()))
    ProxyServer.stop()
    System.exit(0)
  })

  implicit class ExtraNode(n: NodeSeq) {
    def bool = n.headOption.exists(_.text.toBoolean)
    def int = n.headOption.map(_.text.toInt).get

    def string = n.text
  }
}

case class AppConfiguration(name: String,
                            lastModified: Long,
                            proxy: Option[ProxyConfig],
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

class JARAppConfig(val enabled: Boolean, val jar: File, val mainClass: String, val args: Seq[String]) extends ApplicationConfig {
  private var instance: Option[LauncherInstance] = None

  override def start(): Unit = synchronized {
    stop()

    val l = new Launcher(mainClass, Seq(jar), args)
    val li = l.process(jar.getParentFile)
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

class WARAppConfig(enabled: Boolean, war: File, port: Int) extends DependencyAppConfig(
  enabled,
  war.getParentFile,
  "org.eclipse.jetty",
  "jetty-runner",
  "9.3.9.v20160517",
  "org.eclipse.jetty.runner.Runner",
  List("--port", port.toString, war.getCanonicalPath),
  scala = false
)

class DependencyAppConfig(val enabled: Boolean,
                          val workingDirectory: File,
                          val group: String,
                          val artifact: String,
                          val version: String,
                          val mainClass: String,
                          val args: Seq[String],
                          val scala: Boolean = true) extends ApplicationConfig {
  private var instance: Option[LauncherInstance] = None

  override def start(): Unit = synchronized {
    stop()

    val dependency = if (scala) {
      group %% artifact % version
    } else {
      group % artifact % version
    }
    val config = Configuration(dependency, mainClass, args.toArray, workingDirectory = workingDirectory, newProcess = true)
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