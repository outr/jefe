package com.outr.jefe.server

import io.youi.Priority
import io.youi.http._
import io.youi.net.URL
import io.youi.server.KeyStore

case class ProxyConfig(hosts: List[String],
                       destinationHost: String,
                       destinationPort: Int,
                       override val keyStore: Option[KeyStore],
                       override val priority: Priority) extends ProxyHandler {
  private val hostsSet = hosts.map(_.toLowerCase).toSet

  override def matches(url: URL): Boolean = hostsSet.contains(url.host.toLowerCase)

  override def proxy(url: URL): URL = url.copy(host = destinationHost, port = destinationPort)
}