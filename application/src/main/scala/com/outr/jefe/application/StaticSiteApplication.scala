package com.outr.jefe.application

import java.io.File

import io.youi.server.{HttpServerListener, HttpsServerListener, Server}

class StaticSiteApplication(val id: String,
                            directory: File,
                            http: Option[HttpServerListener] = None,
                            https: Option[HttpsServerListener] = None) extends Application {
  assert(http.nonEmpty || https.nonEmpty, "At least one server must be configured!")

  object server extends Server {
    config.listeners := List(http, https).flatten
    handler.file(directory)
  }

  override def start(): Unit = server.start()

  override def isRunning: Boolean = server.isRunning

  override def restart(force: Boolean): Unit = server.restart()

  override def stop(force: Boolean): Unit = server.stop()
}
