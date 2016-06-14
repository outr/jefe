package com.outr.jefe.server

import java.io.File
import java.net.{URI, URL}
import java.nio.file.Path

import com.outr.jefe.launch.{Launcher, LauncherInstance}
import com.outr.jefe.runner.{Arguments, Configuration, Runner}
import pl.metastack.metarx.{Buffer, Sub}

import scala.xml.{Elem, NodeSeq, XML}
import com.outr.jefe.repo._
import com.outr.scribe.Logging
import org.powerscala.io._

object JefeServer {
  // TODO: pick up new directories created in base-dir, use jefe.config files (generate if not defined)
  // TODO: support starting Docker instance
  // TODO: inject communication utility into apps to control and access runtime info
  // TODO: access process ids
  // TODO: support console commands (help, enable, disable, status, apps, shutdown)

  var keepAlive: Boolean = true
  val configurations: Buffer[AppConfiguration] = Buffer()
  var monitor: Monitor = _

  private lazy val thread = new Thread() {
    setDaemon(true)

    override def run(): Unit = {
      while (keepAlive) {
        monitor.poll()
      }
      monitor.dispose()
    }
  }

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

        monitor = new Monitor(directory)

        ProxyServer.start()

        thread.start()    // Start background monitoring thread
      }
      case "stop" => {

      }
      case "status" => {

      }
    }
  }

  def loadConfiguration(directory: File): AppConfiguration = {
    val config = new File(directory, "jefe.config.xml").toURI.toURL
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
          new DependencyAppConfig(enabled, group, artifact, version, mainClass, args)
        }
      }
    }
    AppConfiguration(proxy, app)
  }

  implicit class ExtraNode(n: NodeSeq) {
    def bool = n.headOption.exists(_.text.toBoolean)
    def int = n.headOption.map(_.text.toInt).get

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

class JARAppConfig(val enabled: Boolean, val jar: File, val mainClass: String, val args: Seq[String]) extends ApplicationConfig {
  private var instance: Option[LauncherInstance] = None

  override def start(): Unit = synchronized {
    stop()

    val l = new Launcher(mainClass, Seq(jar), args)
    val li = l.process()
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
  "org.eclipse.jetty",
  "jetty-runner",
  "9.3.9.v20160517",
  "org.eclipse.jetty.runner.Runner",
  List("--port", port.toString, war.getCanonicalPath),
  scala = false
)

class DependencyAppConfig(val enabled: Boolean,
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

class Monitor(directory: File) extends Logging {
  import java.nio.file.StandardWatchEventKinds._
  import scala.collection.JavaConversions._

  private var apps = Map.empty[String, AppConfiguration]

  val path = directory.toPath
  val fileSystem = path.getFileSystem
  val watchService = fileSystem.newWatchService()
  val watchKey = path.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)

  directory.mkdirs()
  if (!directory.isDirectory) throw new IllegalArgumentException(s"${directory.getAbsolutePath} is not a directory.")

  directory.listFiles().foreach { d =>
    if (validDirectory(d)) {
      updateDirectory(d)
    }
  }

  def poll(): Unit = Option(watchService.poll()) match {
    case Some(key) => {
      key.pollEvents().foreach { watchEvent =>
        val path = watchEvent.context().asInstanceOf[Path]
        val file = new File(directory, path.getFileName.toString)
        if (validDirectory(file)) {
          watchEvent.kind() match {
            case OVERFLOW => logger.warn(s"Overflow for ${directory.getAbsolutePath}")
            case ENTRY_CREATE => updateDirectory(file)
            case ENTRY_MODIFY => updateDirectory(file)
            case ENTRY_DELETE => updateDirectory(file)
          }
        } else {
          println(s"File changed: $path")
        }
      }
    }
    case None => // Nothing returned
  }

  def validDirectory(file: File): Boolean = file.isDirectory && !file.isHidden && !file.getName.startsWith(".")

  def updateDirectory(directory: File): Unit = synchronized {
    val name = directory.getName
    if (directory.exists()) {
      val config = new File(directory, "jefe.config.xml")
      if (!config.exists()) {
        IO.stream(getClass.getClassLoader.getResource("template.xml"), config)
      }
      val appConfig = JefeServer.loadConfiguration(directory)
      val oldConfig = apps.get(name)
      oldConfig.foreach(_.application.foreach(_.stop()))    // Shutdown the app if it's running
      appConfig.application.foreach { app =>    // Start the app from the new config
        if (app.enabled) {
          app.start()
        }
      }
      apps += name -> appConfig
      oldConfig match {
        case Some(c) => JefeServer.configurations.replace(c, appConfig)
        case None => JefeServer.configurations += appConfig
      }
      directory.toPath.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
      // TODO: listen to the config file for changes
    } else {
      apps.get(name).foreach(_.application.foreach(_.stop()))
    }
  }

  def dispose(): Unit = {
    watchService.close()
  }

  /*
  val dir = new File(directory, "hyperscala.org")
        val config = loadConfiguration(dir)
        config.application.foreach(_.start())
        configurations += config
   */
}