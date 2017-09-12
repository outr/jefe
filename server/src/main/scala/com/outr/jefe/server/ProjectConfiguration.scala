package com.outr.jefe.server

case class ProjectConfiguration(proxies: List[ProxyConfiguration],
                                applications: List[ApplicationConfiguration],
                                properties: Map[String, String])
