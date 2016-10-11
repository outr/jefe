package com.outr.jefe.server

import java.io.File
import java.net.{InetSocketAddress, URI}

import com.outr.jefe.server.config.InboundDomain
import com.outr.scribe.formatter.Formatter
import com.outr.scribe.writer.FileWriter
import com.outr.scribe.{Level, LogHandler, Logger, Logging}
import io.undertow.{Handlers, Undertow, UndertowOptions}
import io.undertow.server.handlers.proxy.SimpleProxyClientProvider
import io.undertow.server.{HttpHandler, HttpServerExchange}
import io.undertow.util.Headers
import org.hyperscala.{Handler, HandlerBuilder, Priority, Server}
import pl.metastack.metarx.{Buffer, Sub}

import scala.collection.mutable.ListBuffer

object ProxyServer extends Logging {
  val access = new Logger("access", parent = None)
  access.addHandler(LogHandler(Level.Info, Formatter.default, FileWriter.daily("access", new File(JefeServer.directory, "logs/access"))))

  val server = new Server
  def host: Sub[String] = server.config.host
  def port: Sub[Int] = server.config.port
  val password: Sub[String] = Sub("")

  private var handlers = List.empty[Handler]

  init()

  private def init(): Unit = {
    def register(path: String)(f: HttpServerExchange => String): Unit = {
      Handler.pathMatch(path).withHandler("text/plain") { exchange =>
        val credentials = exchange.getQueryParameters.get("password").getFirst
        if (credentials != password.get) {
          "Invalid credentials supplied"
        } else {
          f(exchange)
        }
      }.withPriority(Priority.High).register(server)
    }

    register("/jefe/stop") { exchange =>
      JefeServer.shutdown()
      "Shutting down server..."
    }
    register("/jefe/status") { exchange =>
      JefeServer.status()
    }
    register("/jefe/list") { exchange =>
      JefeServer.list()
    }
    register("/jefe/update") { exchange =>
      JefeServer.updateDirectories()
      "Updating directories..."
    }
    register("/jefe/enable") { exchange =>
      val appNameParams = exchange.getQueryParameters.get("app")
      if (appNameParams.isEmpty) {
        "ERROR: app must be specified!"
      } else {
        val appName = appNameParams.getFirst
        JefeServer.changeEnabled(appName, enable = true)
        s"$appName enabled"
      }
    }
    register("/jefe/disable") { exchange =>
      val appNameParams = exchange.getQueryParameters.get("app")
      if (appNameParams.isEmpty) {
        "ERROR: app must be specified!"
      } else {
        val appName = appNameParams.getFirst
        JefeServer.changeEnabled(appName, enable = false)
        s"$appName disabled"
      }
    }
  }

  JefeServer.configurations.changes.attach { changed =>
    if (server.isStarted) {
      reloadProxies()
    }
  }

  def reloadProxies(): Unit = synchronized {
    // Build the new list of Handlers
    val handlers = ListBuffer.empty[Handler]
    JefeServer.configurations.get.foreach { config =>
      config.proxy match {
        case Some(proxy) if proxy.enabled => {
          var builder: HandlerBuilder = Handler

          // Configure matching
          proxy.inbound.foreach {
            case id: InboundDomain => builder = builder.hostMatch(id.domain)
            case i => throw new RuntimeException(s"Unsupported Inbound: $i.")
          }

          // Configure proxy
          builder = builder.withProxy(proxy.outbound)

          // Configure priority
          builder = builder.withPriority(proxy.priority)

          handlers += builder.build()
        }
        case _ => // No proxy for this configuration or it's disabled
      }
    }

    // Remove all the existing Handlers
    this.handlers.foreach(server.unregister)

    // Register the new list of Handlers
    handlers.foreach(server.register)

    this.handlers = handlers.toList
  }

  def start(): Unit = synchronized {
    stop()

    reloadProxies()

    server.start()
  }

  def stop(): Unit = synchronized {
    server.stop()
  }
}

case class ProxyMapping()