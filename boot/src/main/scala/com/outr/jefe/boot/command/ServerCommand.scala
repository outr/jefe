package com.outr.jefe.boot.command

import com.outr.jefe.application.ArtifactApplication
import com.outr.jefe.client.JefeClient
import com.outr.jefe.server.JefeServer
import io.youi.net.URL
import io.youi.server.ServerUtil
import profig.Profig
import com.outr.jefe.resolve._

import scala.concurrent.Await
import scala.concurrent.duration._

object ServerCommand extends Command {
  override def name: String = "server"
  override def description: String = "Starts the Jefe server that manages a stateful list of applications"

  lazy val client = new JefeClient(URL(s"http://${JefeServer.host}:${JefeServer.port}"), JefeServer.token)

  override def execute(): Unit = Profig("arg2").opt[String] match {
    case Some("start") => start()
    case Some("stop") => stop()
    case Some("restart") => restart()
    case Some(command) => {
      logger.info(s"Unsupported server command: $command")
      logger.info("")
      help()
    }
    case None => {
      logger.info("A server command must be supplied!")
      logger.info("")
      help()
    }
  }

  def start(blocking: Boolean = Profig("blocking").opt[String].exists(_.toBoolean)): Unit = {
    assert(ServerUtil.isPortAvailable(JefeServer.port, JefeServer.host), s"${JefeServer.host}:${JefeServer.port} is already in use")
    if (blocking) {
      JefeServer.start()
    } else {
      val version = Profig("version").as[String]("latest.release")
      val artifacts = List("com.outr" % "jefe-server_2.12" % version)
      val app = ArtifactApplication(
        id = "jefe-server",
        artifacts = artifacts,
        mainClass = Some("com.outr.jefe.server.JefeServer"),
        background = true
      )
      app.start()
    }
  }

  def stop(): Unit = {
    val response = Await.result(client.stop(), 5.seconds)
    assert(response.success, s"Response was not successful! Errors: ${response.errors}")
    scribe.info("Stop request was successful")
    sys.exit(0)
  }

  def restart(): Unit = {
    Await.result(client.stop(), 15.seconds)
    start(blocking = false)
  }

  override def help(): Unit = {
    logger.info("Usage: jefe server [start|stop|restart]")
    logger.info("")
    logger.info("Arguments:")
    logger.info("  --blocking=[true|false]: Optionally specifies if starting the server to occur in a background process (false) or in the current process (true). Defaults to false.")
    logger.info("  --listeners.http.port=???: Specifies the port that the server should run on. Defaults to 10565.")
    logger.info("  --jefe.token=???: Specifies the services token for security. If unspecified one will be generated at startup and printed to the logs (changes each startup).")
    // TODO: support baseDirectory that defaults to `~/.jefe/` if unspecified
  }
}