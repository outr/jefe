package com.outr.jefe.boot.command

import java.io.File

import com.outr.jefe.application.StaticSiteApplication
import io.youi.server.HttpServerListener
import profig.Profig

object ServeCommand extends Command {
  override def name: String = "serve"
  override def description: String = "Serves static content on a host and port"

  override def execute(): Unit = {
    val path = Profig("path").opt[String]
      .orElse(Profig("arg2").opt[String])
      .map(new File(_)).getOrElse(new File("."))
    val host = Profig("host").opt[String].getOrElse("localhost")
    val port = Profig("port").opt[Int].getOrElse(8080)
    val application = new StaticSiteApplication("serve", path, Some(HttpServerListener(host, port)))
    logger.info(s"Serving ${path.getCanonicalPath}...")
    application.start()
  }

  override def help(): Unit = {
    logger.info("Usage: jefe serve /server/path/ --host=localhost --port=8080")
    logger.info("")
    logger.info("Arguments:")
    logger.info("  --path=???: Sets the path to serve files from. If unspecified, it will use the current path. May be specified as an unlabeled argument or named.")
    logger.info("  --host=???: Sets the hostname or IP address to bind to. If unspecified, it will use the default (localhost).")
    logger.info("  --port=???: Sets the port to bind to. If unspecified, it will use the default port (8080).")
  }
}
