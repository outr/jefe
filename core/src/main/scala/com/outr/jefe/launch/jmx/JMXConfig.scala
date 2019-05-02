package com.outr.jefe.launch.jmx

import org.powerscala.util.NetUtil

import scala.annotation.tailrec

case class JMXConfig(port: Int = JMXConfig.nextPort(), host: String = "127.0.0.1") {
  lazy val args: List[String] = List(
    "-Dcom.sun.management.jmxremote=true",
    s"-Djava.rmi.server.hostname=$host",
    s"-Dcom.sun.management.jmxremote.port=$port",
    "-Dcom.sun.management.jmxremote.authenticate=false",
    "-Dcom.sun.management.jmxremote.ssl=false",
    "-Dcom.sun.management.jmxremote.local.only=false"
  )
}

object JMXConfig {
  private var counter = 10000

  def nextPort(): Int = synchronized(nextPortRecursive())

  @tailrec
  private def nextPortRecursive(): Int = {
    val port = counter
    counter += 1
    if (NetUtil.isTCPPortFree(port)) {
      port
    } else {
      nextPortRecursive()
    }
  }
}