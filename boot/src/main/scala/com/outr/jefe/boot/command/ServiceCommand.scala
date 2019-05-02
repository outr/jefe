package com.outr.jefe.boot.command

import java.io.File

import org.powerscala.io._
import profig.Profig

object ServiceCommand extends Command {
  private lazy val userName: String = System.getProperty("user.name")
  private lazy val homeDir: String = System.getProperty("user.home")
  private lazy val script: String =
    s"""
      |[Unit]
      |Description=Jefe server for management of multiple server processes
      |Documentation=https://github.com/outr/jefe
      |
      |[Service]
      |Type=simple
      |User=$userName
      |WorkingDirectory=$homeDir/.jefe
      |ExecStart=/usr/bin/jefe server start --blocking=true
      |ExecStop=/usr/bin/jefe server stop
      |
      |[Install]
      |WantedBy=multi-user.target
    """.stripMargin

  override def name: String = "service"

  override def description: String = "Manages running Jefe as a OS-level service"

  override def execute(): Unit = Profig("arg2").opt[String] match {
    case Some(command) => command match {
      case "install" => install()
      case "uninstall" => uninstall()
      case "enable" => enable()
      case "disable" => disable()
      case "start" => start()
      case "stop" => stop()
      case _ => {
        logger.info(s"Invalid command: $command")
        logger.info("")
        help()
      }
    }
    case None => {
      logger.info("A command must be specified for service!")
      logger.info("")
      help()
    }
  }

  def install(): Unit = {
    logger.info(s"Installing service for user $userName...")
    val temp = File.createTempFile("jefe", ".service")
    IO.stream(script, temp)
    process("/usr/bin/sudo", "/bin/cp", temp.getCanonicalPath, "/lib/systemd/system/jefe.service")
    process("/usr/bin/sudo", "systemctl", "daemon-reload")
    logger.info("Successfully installed the service for Jefe")
    enable()
  }

  def enable(): Unit = {
    process("/usr/bin/sudo", "systemctl", "enable", "jefe.service")
    logger.info("Successfully enabled the service for Jefe")
  }

  def disable(): Unit = {
    process("/usr/bin/sudo", "systemctl", "disable", "jefe.service")
    logger.info("Successfully disabled the service for Jefe")
  }

  def uninstall(): Unit = {
    process("/usr/bin/sudo", "/bin/rm", "-f", "/lib/systemd/system/jefe.service")
    process("/usr/bin/sudo", "systemctl", "daemon-reload")
    logger.info("Successfully uninstalled the service for Jefe")
  }

  def start(): Unit = {
    process("/usr/bin/sudo", "service", "jefe", "start")
    logger.info("Successfully started the service for Jefe")
  }

  def stop(): Unit = {
    process("/usr/bin/sudo", "service", "jefe", "stop")
    logger.info("Successfully stopped the service for Jefe")
  }

  def process(commands: String*): Unit = {
    val pb = new ProcessBuilder(commands: _*)
    pb.inheritIO()
    val p = pb.start()
    val exitValue = p.waitFor()
    if (exitValue != 0) {
      logger.info(s"Execution of command: ${commands.mkString(" ")} failed with $exitValue!")
      sys.exit(exitValue)
    }
  }

  override def help(): Unit = {
    logger.info("Usage: jefe service install|uninstall|enable|disable|start|stop")
    logger.info("")
    logger.info("This service will be installed and run as the user this command is run under.")
    logger.info("This will invoke sudo to configure the service, so do not manually call sudo for this command.")
  }
}
