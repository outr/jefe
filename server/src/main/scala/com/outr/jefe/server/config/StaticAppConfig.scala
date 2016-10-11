package com.outr.jefe.server.config

import java.io.File

import org.hyperscala.Server

class StaticAppConfig(val enabled: Boolean, host: String, port: Int, directory: File) extends ApplicationConfig {
  private val server = new Server
  server.config.host := host
  server.config.port := port
  server.resourceManager.file(directory)()

  override def start(): Unit = {
    server.start()
  }

  override def stop(): Unit = {
    server.stop()
  }
}