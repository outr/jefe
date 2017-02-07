package com.outr.jefe.server.config

import java.io.File

import io.youi.Priority
import io.youi.net.URL
import io.youi.server.KeyStore

case class ProxyConfig(enabled: Boolean = false,
                       inboundPort: Option[Int],
                       inbound: List[Inbound],
                       outbound: Outbound,
                       priority: Priority)

case class Outbound(url: URL, keyStore: Option[KeyStore])