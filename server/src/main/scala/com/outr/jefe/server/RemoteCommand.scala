package com.outr.jefe.server

case class RemoteCommand(value: String, password: Option[String], base: String)
