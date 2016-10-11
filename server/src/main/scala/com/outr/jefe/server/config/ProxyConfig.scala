package com.outr.jefe.server.config

import java.net.URI

import org.hyperscala.Priority

case class ProxyConfig(enabled: Boolean = false,
                       inbound: List[Inbound],
                       outbound: URI,
                       priority: Priority)