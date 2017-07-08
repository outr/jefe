package com.outr.jefe.server

import java.io.File
import java.net.URLEncoder

import com.outr.jefe.runner.{Arguments, Repositories}

import scala.xml.{Elem, Node, NodeSeq, XML}
import com.outr.jefe.server.config._
import reactify.Var
import scribe.writer.FileWriter
import scribe.{LogHandler, Logger, Logging}
import io.youi.Priority
import io.youi.net.URL
import io.youi.server.KeyStore
import org.powerscala.StringUtil
import org.powerscala.concurrent.Time
import org.powerscala.io._
import org.powerscala.util.NetUtil

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.transform.RewriteRule

object JefeServer extends Logging {
  val started: Long = System.currentTimeMillis()

  val configurations: Var[List[AppConfiguration]] = Var[List[AppConfiguration]](Nil)
  var directory: File = _

  def mainOld(args: Array[String]): Unit = {
    if (args.isEmpty) {
      println(
        """Usage: jefe <command> (options)
          | Commands:
          |   start - starts the server instance
          |     password - the password required for interaction. defaults to "".
          |     host - the host to bind to. defaults to "0.0.0.0".
          |     port - the port to bind to. defaults to 8080.
          |     ssl.keystore - the path to the JKS SSL keystore file (Optional. Enables SSL if defined).
          |     ssl.host - the host to bind HTTPS to. defaults to "0.0.0.0".
          |     ssl.port - the port to bind HTTPS to. defaults to 8443.
          |     ssl.password - the keystore password to use. defaults to "password".
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
    val host = arguments.takeOrElse("host", "0.0.0.0")
    val port = arguments.takeOrElse("port", "8080").toInt
    val sslKeyStore = arguments.takeOptional("ssl.keystore")
    val sslHost = arguments.takeOrElse("ssl.host", "0.0.0.0")
    val sslPort = arguments.takeOrElse("ssl.port", "8443").toInt
    val sslPassword = arguments.takeOrElse("ssl.password", "password")

    directory = new File(arguments.takeOrElse("directory", "."))
    val app = arguments.takeOrElse("app", "")

    Logger.root.addHandler(LogHandler(writer = FileWriter.daily("jefe", new File(directory, "logs"))))

    action match {
      case "start" => {
        ProxyServer.config.clearListeners()
        ProxyServer.config.addHttpListener(host, port)
        sslKeyStore.foreach { path =>
          ProxyServer.config.addHttpsListener(sslHost, sslPort, sslPassword, new File(path))
        }
        ProxyServer.password := password

        updateDirectories()

        ProxyServer.start()

        CommandSupport.init()
      }
      case "stop" => send(host, port, password, "stop", Map.empty)
      case "status" => send(host, port, password, "status", Map("app" -> app))
      case "list" => send(host, port, password, "list", Map.empty)
      case "update" => send(host, port, password, "update", Map.empty)
      case "enable" => send(host, port, password, "enable", Map("app" -> app))
      case "disable" => send(host, port, password, "disable", Map("app" -> app))
    }
  }

  def send(host: String, port: Int, password: String, command: String, args: Map[String, String]): Unit = {
    val argsString = (args + ("password" -> password)).map(t => s"${t._1}=${URLEncoder.encode(t._2, "UTF-8")}").mkString("&")
    val url = new java.net.URL(s"http://$host:$port/jefe/$command?$argsString")
    val response = IO.stream(url.openStream(), new StringBuilder).toString
    println(response)
  }

  def status(): String = {
    var heapCommitted = 0L
    var heapUsed = 0L
    var offheapCommitted = 0L
    var offheapUsed = 0L
    val entries = configurations.get.map { appConfig =>
      val (stats, pid) = appConfig.application.collect {
        case app: ProcessApplicationConfig => app.processMonitor.map(_.stats()).map { processStats =>
          heapCommitted += processStats.heapUsage.committed
          heapUsed += processStats.heapUsage.used
          offheapCommitted += processStats.nonHeapUsage.committed
          offheapUsed += processStats.nonHeapUsage.used
          processStats.toString.trim -> app.pid.getOrElse(-1)
        }
      }.flatten.getOrElse("" -> -1)
      s"${appConfig.name}: PID=$pid\n\t$stats"
    }.mkString("\n\n")

    entries +
      s"""
         |
         |Total Heap Committed: ${StringUtil.humanReadableByteCount(heapCommitted)}
         |Total Heap Used: ${StringUtil.humanReadableByteCount(heapUsed)}
         |Total Off-Heap Committed: ${StringUtil.humanReadableByteCount(offheapCommitted)}
         |Total Off-Heap Used: ${StringUtil.humanReadableByteCount(offheapUsed)}
         |Uptime: ${Time.elapsed(System.currentTimeMillis() - started).toString()}
       """.stripMargin
  }

  def list(): String = {
    val heading = s"Listing ${configurations.get.size} configuration(s):"
    val items = configurations.get.map(appConfig => s"${appConfig.name} (pid: ${appConfig.application.map(ProcessApplicationConfig.pid)}): ${appConfig.proxies} / ${appConfig.application}").mkString("\n")
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
          case Some(c) => JefeServer.configurations := JefeServer.configurations().map {
            case cfg if cfg == c => appConfig
            case cfg => cfg
          }
          case None => JefeServer.configurations := JefeServer.configurations() ::: List(appConfig)
        }
      }
    } else {
      configurations.get.find(_.name == name).foreach(_.application.foreach(_.stop()))
    }
  }

  def changeEnabled(appName: String, enable: Boolean): Unit = {
    val directory = new File(this.directory, appName)
    val configFile = new File(directory, "jefe.config.xml")
    val config = configFile.toURI.toURL
    val xml = XML.load(config)
    val rewriteRule = new RewriteRule {
      override def transform(n: Node): Seq[Node] = n match {
        case <enabled>{b}</enabled> => <enabled>{enable}</enabled>
        case e: Elem => e.copy(child = transform(e.child))
        case _ => n
      }
    }
    val updated = rewriteRule.transform(xml).head
    XML.save(configFile.getAbsolutePath, updated)
    updateDirectories()
  }

  def loadConfiguration(directory: File): AppConfiguration = {
    val name = directory.getName
    val configFile = new File(directory, "jefe.config.xml")
    val lastModified = configFile.lastModified()
    val config = configFile.toURI.toURL
    val xml = XML.load(config)
    val proxies = (xml \ "proxy").map { p =>
      val enabled = (p \ "enabled").bool
      val inboundXML = p \ "inbound"
      val inboundPort = (inboundXML \ "@port").intOption
      val inbound = inboundXML.head.flatMap(_.child.collect {
        case e: Elem => e.label match {
          case "domain" => InboundDomain(e.text)
          case label => throw new RuntimeException(s"Unsupported inbound type: $label.")
        }
      }).toList
      val outboundXML = p \ "outbound"
      val outboundURI = URL(outboundXML.text)
      val keyStore = (outboundXML \ "@keyStore").stringOption.map(p => KeyStore(p, (outboundXML \ "@password").string))
      val outbound = Outbound(outboundURI, keyStore)
      val priority = Priority.byName((p \ "priority").text).getOrElse(Priority.Normal)
      ProxyConfig(enabled, inboundPort, inbound, outbound, priority)
    }.toList
    val app = (xml \ "application").headOption.map { a =>
      val enabled = (a \ "enabled").bool
      val mainClass = (a \ "mainClass").string
      val args = (a \ "arg").map(_.text)
      val jmxPort = NetUtil.availableTCPPort()
      val additionalArgs = List(
        "-Dcom.sun.management.jmxremote=true",
        "-Djava.rmi.server.hostname=127.0.0.1",
        s"-Dcom.sun.management.jmxremote.port=$jmxPort",
        "-Dcom.sun.management.jmxremote.authenticate=false",
        "-Dcom.sun.management.jmxremote.ssl=false",
        "-Dcom.sun.management.jmxremote.local.only=false"
      )
      val vmArgs = additionalArgs ::: (a \ "vmargs").map(_.text).toList
      (a \ "type").string match {
        case "jar" => {
          val jar = new File(directory, (a \ "jar").string)
          if (!jar.exists()) {
            throw new RuntimeException(s"JAR doesn't exist: ${jar.getAbsolutePath}")
          }
          new JARAppConfig(enabled, jar, mainClass, args, jmxPort, vmArgs)
        }
        case "war" => {
          val war = new File(directory, (a \ "war").string)
          if (!war.exists()) {
            throw new RuntimeException(s"WAR doesn't exist: ${war.getAbsolutePath}")
          }
          val port = (a \ "port").int
          new WARAppConfig(enabled, war, port, jmxPort, vmArgs)
        }
        case "dependency" => {
          val group = (a \ "group").string
          val artifact = (a \ "artifact").string
          val version = (a \ "version").string
          val scalaVersion = (a \ "scalaVersion").headOption.map(_.text)
          new DependencyAppConfig(enabled, directory, group, artifact, version, mainClass, args, jmxPort, vmArgs, Repositories(), scalaVersion)
        }
        case "static" => {
          val path = (a \ "path").string
          val contentDirectory = if (path.isEmpty) {
            new File(directory, "content")
          } else {
            new File(directory, path)
          }
          val host = (a \ "host").string match {
            case "" => "127.0.0.1"
            case h => h
          }
          val port = (a \ "port").int
          new StaticAppConfig(enabled, host, port, contentDirectory)
        }
      }
    }
    AppConfiguration(name, lastModified, proxies, app)
  }

  def shutdown(): Unit = {
    Thread.sleep(1000)      // Wait for one second
    configurations.foreach(_.application.foreach(_.stop()))
    ProxyServer.stop()
    System.exit(0)
  }

  implicit class ExtraNode(n: NodeSeq) {
    def bool: Boolean = n.headOption.exists(_.text.toBoolean)
    def int: Int = n.headOption.map(_.text.toInt).get
    def intOption: Option[Int] = n.headOption.map(_.text.toInt)

    def string: String = n.text
    def stringOption: Option[String] = n.text match {
      case s if s.trim.nonEmpty => Some(s)
      case _ => None
    }
  }
}