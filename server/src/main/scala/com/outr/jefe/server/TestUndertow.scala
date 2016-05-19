package com.outr.jefe.server

import java.net.URI

import io.undertow.server.handlers.proxy.SimpleProxyClientProvider
import io.undertow.{Handlers, Undertow}
import io.undertow.server.{HttpHandler, HttpServerExchange}
import io.undertow.util.Headers

object TestUndertow extends App {
  val proxyClient = new SimpleProxyClientProvider(new URI("http://hyperscala.org"))
  val proxy = Handlers.proxyHandler(proxyClient)
  val mutator = new HttpHandler {
    override def handleRequest(exchange: HttpServerExchange): Unit = {
      exchange.getRequestHeaders.put(Headers.HOST, s"hyperscala.org")
      proxy.handleRequest(exchange)
    }
  }
  val server = Undertow.builder()
    .addHttpListener(8080, "localhost")
    .setHandler(mutator)
    .build()
  server.start()
}