package com.outr.jefe.server.config

import java.io.File
import java.net.URI

import org.hyperscala.Priority

case class ProxyConfig(enabled: Boolean = false,
                       inboundPort: Option[Int],
                       inbound: List[Inbound],
                       outbound: Outbound,
                       priority: Priority)

case class Outbound(uri: URI, keyStore: Option[File], password: String)