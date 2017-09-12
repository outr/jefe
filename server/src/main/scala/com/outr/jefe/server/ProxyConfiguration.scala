package com.outr.jefe.server

case class ProxyConfiguration(enabled: Boolean, inbound: ProxyInboundConfiguration, outbound: String)
