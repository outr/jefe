package com.outr.jefe.server

import io.youi.server.KeyStore

case class ProxyConfiguration(enabled: Boolean, inbound: ProxyInboundConfiguration, outbound: ProxyOutboundConfiguration)

case class ProxyOutboundConfiguration(url: String, keyStore: Option[KeyStore])