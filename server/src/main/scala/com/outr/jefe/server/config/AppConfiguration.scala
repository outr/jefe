package com.outr.jefe.server.config

case class AppConfiguration(name: String,
                            lastModified: Long,
                            proxies: List[ProxyConfig],
                            application: Option[ApplicationConfig])
