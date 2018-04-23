package com.outr.jefe.server

case class MainConfiguration(host: Option[String] = None,
                             port: Option[Int] = None,
                             startServer: Option[Boolean] = None,
                             ssl: Option[SSLConfiguration] = None,
                             password: Option[String] = None,
                             paths: List[String] = Nil,
                             useCoursier: Boolean = true)
