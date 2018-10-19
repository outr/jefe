package com.outr.jefe.boot.command

import com.outr.jefe.model.BasicResponse
import com.outr.jefe.server.ProxyConfig
import io.youi.Priority
import io.youi.server.KeyStore
import profig.Profig

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object ProxyCommand extends Command {
  override def name: String = "proxy"
  override def description: String = "Manages proxies on the server"

  override def execute(): Unit = {
    val hosts = Profig("hosts").opt[String].map(_.split(',').toList)
    val destinationHost = Profig("destinationHost").opt[String]
    val destinationPort = Profig("destinationPort").opt[String].map(_.toInt)
    val keystorePath = Profig("keystorePath").opt[String]
    val keystorePassword = Profig("keystorePassword").as[String]("password")

    lazy val proxyConfig: Option[ProxyConfig] = if (hosts.isEmpty) {
      logger.info("At least one host must be specified in the --hosts argument")
      logger.info("")
      help()
      None
    } else if (destinationHost.isEmpty || destinationPort.isEmpty) {
      logger.info("Both --destinationHost and --destinationPort must be defined!")
      logger.info("")
      help()
      None
    } else {
      Some(ProxyConfig(
        hosts = hosts.getOrElse(Nil),
        destinationHost = destinationHost.getOrElse(""),
        destinationPort = destinationPort.getOrElse(-1),
        keyStore = keystorePath.map(path => KeyStore(path, keystorePassword)),
        priority = Priority.High
      ))
    }

    def handle(command: String, handler: ProxyConfig => Future[BasicResponse]): Unit = proxyConfig.foreach { config =>
      val future = handler(config)
      val response = Await.result(future, 15.seconds)
      assert(response.success, s"Response was not successful! Errors: ${response.errors}")
      logger.info(s"Proxy $command completed successfully")
      sys.exit(0)
    }

    Profig("arg2").opt[String] match {
      case Some(command) => command match {
        case "add" => handle(command, ServerCommand.client.proxy.add)
        case "remove" => handle(command, ServerCommand.client.proxy.remove)
        case _ => {
          logger.info(s"Invalid command: $command")
          logger.info("")
          help()
        }
      }
      case None => {
        logger.info("A command must be specified!")
        logger.info("")
        help()
      }
    }
  }

  override def help(): Unit = {
    logger.info("Usage: jefe proxy add|remove")
    logger.info("")
    logger.info("Arguments:")
    logger.info("  --hosts=???: Comma separated list of hostnames to match this proxy on (origin)")
    logger.info("  --destinationHost=???: The destination host to forward a matched request (destination)")
    logger.info("  --destinationPort=???: The destination port to forward a matched request (destination)")
    logger.info("  --keystorePath=???: Optionally defines the path to a JKS file to apply to the proxy (defaults to none)")
    logger.info("  --keystorePassword=???: Optionally defines the password to a JKS file to apply to the proxy (defaults to \"password\" if keystorePath is set)")
  }
}