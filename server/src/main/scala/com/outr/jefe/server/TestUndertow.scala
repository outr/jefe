package com.outr.jefe.server

import java.net.URI

import io.undertow.server.handlers.proxy.SimpleProxyClientProvider
import io.undertow.{Handlers, Undertow}
import io.undertow.server.{HttpHandler, HttpServerExchange}
import io.undertow.util.Headers

object TestUndertow extends App {
  // TODO: pick up new directories created in base-dir, use jefe.config files (generate if not defined)
  // TODO: support basic proxying
  // TODO: support starting application (dependency management)
  // TODO: support starting JAR
  // TODO: support starting WAR
  // TODO: inject communication utility into apps to control and access runtime info
  // TODO: access process ids
  // TODO: support console commands (help, enable, disable, status, apps, shutdown)
  val proxyClient = new SimpleProxyClientProvider(new URI("http://74.208.78.134"))
  val proxy = Handlers.proxyHandler(proxyClient)
//  val mutator = new HttpHandler {
//    override def handleRequest(exchange: HttpServerExchange): Unit = {
//      exchange.getRequestHeaders.put(Headers.HOST, s"hyperscala.org")
//      proxy.handleRequest(exchange)
//    }
//  }
  val server = Undertow.builder()
    .addHttpListener(8080, "localhost")
    .setHandler(proxy)
    .build()
  server.start()
}