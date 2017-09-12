package com.outr.jefe.server.config

import java.io.File

import io.youi.server.Server

class StaticAppConfig(val enabled: Boolean, host: String, port: Int, directory: File) extends ApplicationConfig {
  private object server extends Server
  server.config.clearListeners().addHttpListener(host, port)
  server.handler.file(directory)

  override def start(): Unit = {
    server.start()
  }

  override def stop(): Unit = {
    server.stop()
  }
}