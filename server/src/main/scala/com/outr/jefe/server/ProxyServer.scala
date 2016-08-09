package com.outr.jefe.server

import java.io.File
import java.net.{InetSocketAddress, URI}

import com.outr.jefe.server.config.InboundDomain
import com.outr.scribe.formatter.Formatter
import com.outr.scribe.writer.FileWriter
import com.outr.scribe.{Level, LogHandler, Logger, Logging}
import io.undertow.{Handlers, Undertow}
import io.undertow.server.handlers.proxy.SimpleProxyClientProvider
import io.undertow.server.{HttpHandler, HttpServerExchange}
import io.undertow.util.Headers
import pl.metastack.metarx.{Buffer, Sub}

object ProxyServer extends Logging {
  val access = new Logger("access", parent = None)
  access.addHandler(LogHandler(Level.Info, Formatter.Default, FileWriter.Daily("access", new File(JefeServer.directory, "logs/access"))))

  val host: Sub[String] = Sub("localhost")
  val port: Sub[Int] = Sub(8080)
  val password: Sub[String] = Sub("")

  private var domainProxies = Map.empty[String, HttpHandler]

  private val instance: Sub[Option[Undertow]] = Sub(None)
  private object handler extends HttpHandler {
    override def handleRequest(exchange: HttpServerExchange): Unit = {
      exchange.getRequestPath match {
        case "/jefe/stop" => authAction(exchange) {
          JefeServer.shutdown()
          "Shutting down server..."
        }
        case "/jefe/status" => authAction(exchange) {
          JefeServer.status()
        }
        case "/jefe/list" => authAction(exchange) {
          JefeServer.list()
        }
        case "/jefe/update" => authAction(exchange) {
          JefeServer.updateDirectories()
          "Updating directories..."
        }
        case "/jefe/enable" => authAction(exchange) {
          val appNameParams = exchange.getQueryParameters.get("app")
          if (appNameParams.isEmpty) {
            "ERROR: app must be specified!"
          } else {
            val appName = appNameParams.getFirst
            JefeServer.changeEnabled(appName, enable = true)
            s"$appName enabled"
          }
        }
        case "/jefe/disable" => authAction(exchange) {
          val appNameParams = exchange.getQueryParameters.get("app")
          if (appNameParams.isEmpty) {
            "ERROR: app must be specified!"
          } else {
            val appName = appNameParams.getFirst
            JefeServer.changeEnabled(appName, enable = false)
            s"$appName disabled"
          }
        }
        case _ => {
          val found = domainProxies.get(exchange.getHostName) match {
            case Some(handler) => {
              handler.handleRequest(exchange)
              true
            }
            case None => {
              logger.warn(s"No match for proxying domain: ${exchange.getRequestURL}")
              false
            }
          }
          val peer = exchange.getConnection.getPeerAddress.asInstanceOf[InetSocketAddress]
          access.info(s"Handler: ${if (found) "Found" else "Not found"}, URL: ${exchange.getRequestURL}, Client: ${peer.getHostName} (${peer.getAddress})")
        }
      }
    }
  }

  def authAction(exchange: HttpServerExchange)(f: => String): Unit = {
    val credentials = exchange.getQueryParameters.get("password").getFirst
    val message = if (credentials != password.get) {
      "Invalid credentials supplied"
    } else {
      f
    }
    exchange.getResponseHeaders.put(Headers.CONTENT_TYPE, "text/plain")
    exchange.getResponseSender.send(message)
  }

  JefeServer.configurations.changes.attach { changed =>
    if (instance.get.nonEmpty) {
      reloadProxies()
    }
  }

  def reloadProxies(): Unit = {
    var dp = Map.empty[String, HttpHandler]
    JefeServer.configurations.get.foreach { config =>
      config.proxy match {
        case Some(proxy) if proxy.enabled => {
          val provider = new SimpleProxyClientProvider(proxy.outbound)
          val client = Handlers.proxyHandler(provider)
          proxy.inbound.foreach {
            case id: InboundDomain => dp += id.domain -> client
            case i => throw new RuntimeException(s"Unsupported Inbound: $i.")
          }
        }
        case _ => // No proxy for this config or disabled
      }
    }
    domainProxies = dp
  }

  def start(): Unit = synchronized {
    stop()

    reloadProxies()

    val server = Undertow.builder()
      .addHttpListener(port.get, host.get)
      .setHandler(handler)
      .build()
    server.start()
    instance := Some(server)
  }

  def stop(): Unit = synchronized {
    instance.get match {
      case Some(i) => {
        i.stop()
        instance := None
      }
      case None => // Nothing existing
    }
  }
}