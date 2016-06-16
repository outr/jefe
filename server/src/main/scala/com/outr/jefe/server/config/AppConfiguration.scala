package com.outr.jefe.server.config

case class AppConfiguration(name: String,
                            lastModified: Long,
                            proxy: Option[ProxyConfig],
                            application: Option[ApplicationConfig])
