//package com.outr.jefe.server
//
//import java.io.File
//import java.net.{InetSocketAddress, URI}
//
//import com.outr.jefe.server.config.InboundDomain
//import reactify.{ChangeListener, Var}
//import scribe.formatter.Formatter
//import scribe.writer.FileWriter
//import scribe.{Level, LogHandler, Logger, Logging}
//import io.youi.{Priority, http}
//import io.youi.http.{Content, HttpConnection, HttpRequest, ProxyHandler}
//import io.youi.net.{ContentType, URL, URLMatcher}
//import io.youi.server._
//import io.youi.server.handler.{CachingManager, HttpHandler, HttpHandlerBuilder}
//
//import scala.collection.mutable.ListBuffer
//
//object ProxyServer extends UndertowServer with Logging {
//  val access = new Logger(parentName = None)
//  access.addHandler(LogHandler(Level.Info, Formatter.default, FileWriter.daily("access", new File(JefeServer.directory, "logs/access"))))
//
//  val password: Var[String] = Var("")
//
//  private var proxyHandlers = List.empty[HttpHandler]
//
//  override protected def init(): Unit = {
//    super.init()
//
//    def register(path: String)(f: HttpRequest => String): Unit = {
//      handler.matcher(http.path.exact(path)).priority(Priority.High).caching(CachingManager.NotCached).handle { httpConnection =>
//        val request = httpConnection.request
//        val credentials = request.url.param("password").getOrElse("")
//        val contentString = if (credentials != password()) {
//          "Invalid credentials supplied"
//        } else {
//          f(request)
//        }
//        httpConnection.update(_.withContent(Content.string(contentString, ContentType.`text/plain`)))
//      }
//    }
//
//    register("/jefe/stop") { _ =>
//      JefeServer.shutdown()
//      "Shutting down server..."
//    }
//    register("/jefe/status") { _ =>
//      JefeServer.status()
//    }
//    register("/jefe/list") { _ =>
//      JefeServer.list()
//    }
//    register("/jefe/update") { _ =>
//      JefeServer.updateDirectories()
//      "Updating directories..."
//    }
//    register("/jefe/enable") { request =>
//      request.url.param("app") match {
//        case Some(appName) => {
//          JefeServer.changeEnabled(appName, enable = true)
//          s"$appName enabled"
//        }
//        case None => "ERROR: app must be specified!"
//      }
//    }
//    register("/jefe/disable") { request =>
//      request.url.param("app") match {
//        case Some(appName) => {
//          JefeServer.changeEnabled(appName, enable = false)
//          s"$appName disabled"
//        }
//        case None => "ERROR: app must be specified!"
//      }
//    }
//  }
//
//  JefeServer.configurations.attach { _ =>
//    if (isRunning) {
//      reloadProxies()
//    }
//  }
//
//  def reloadProxies(): Unit = synchronized {
//    // Build the new list of Handlers
//    val handlers = ListBuffer.empty[HttpHandler]
//    JefeServer.configurations.get.foreach { config =>
//      config.proxies.foreach { proxyConfig =>
//        if (proxyConfig.enabled) {
//          // Configure matching
//          val matchers = proxyConfig.inbound.map {
//            case id: InboundDomain if proxyConfig.inboundPort.nonEmpty => {
//              new URLMatcher {
//                override def matches(url: URL): Boolean = url.host.matches(id.domain) && url.port == proxyConfig.inboundPort.get
//              }
//            }
//            case id: InboundDomain => http.host.matches(id.domain)
//            case i => throw new RuntimeException(s"Unsupported Inbound: $i.")
//          }
//
//          // Configure proxy
//          val httpHandler = handler.matcher(http.combined.any(matchers: _*)).priority(proxyConfig.priority).proxy(new ProxyHandler {
//            override def proxy(connection: HttpConnection): Option[URL] = Some(proxyConfig.outbound.url)
//
//            override def keyStore: Option[KeyStore] = proxyConfig.outbound.keyStore
//          })
//
//          handlers += httpHandler
//        }
//      }
//    }
//
//    // Remove all the existing Handlers
//    this.proxyHandlers.foreach(h => handlers -= h)
//
//    this.proxyHandlers = handlers.toList
//  }
//
//  override def start(): Unit = synchronized {
//    stop()
//
//    reloadProxies()
//
//    super.start()
//  }
//}