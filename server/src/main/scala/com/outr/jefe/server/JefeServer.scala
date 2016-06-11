package com.outr.jefe.server

import java.io.File
import java.net.URL

import scala.xml.{Elem, NodeSeq, XML}

object JefeServer {
  def main(args: Array[String]): Unit = {
    val config = loadConfiguration(getClass.getClassLoader.getResource("template.xml"))
    println(config)
  }

  def loadConfiguration(config: URL): Configuration = {
    val xml = XML.load(config)
    val proxy = (xml \ "proxy").headOption.map { p =>
      val enabled = (p \ "enabled").bool
      val inbound = (p \ "inbound").head.flatMap(_.child.collect {
        case e: Elem => e.label match {
          case "domain" => InboundDomain(e.text)
          case label => throw new RuntimeException(s"Unsupported inbound type: $label.")
        }
      }).toList
      val outbound = (p \ "outbound").headOption.map { n =>
        Outbound((n \ "host").text, (n \ "port").text.toInt)
      }
      ProxyConfig(enabled, inbound, outbound)
    }
    val app = (xml \ "application").headOption.map { a =>
      val enabled = (a \ "enabled").bool
      val mainClass = (a \ "mainClass").string
      val args = (a \ "arg").map(_.text)
      (a \ "type").string match {
        case "dependency" => {
          val group = (a \ "group").string
          val artifact = (a \ "artifact").string
          val version = (a \ "version").string
          DependencyAppConfig(enabled, group, artifact, version, mainClass, args)
        }
      }
    }
    Configuration(proxy, app)
  }

  implicit class ExtraNode(n: NodeSeq) {
    def bool = n.headOption.exists(_.text.toBoolean)
    def string = n.text
  }
}

case class Configuration(proxy: Option[ProxyConfig],
                         application: Option[ApplicationConfig])

case class ProxyConfig(enabled: Boolean = false,
                       inbound: List[Inbound],
                       outbound: Option[Outbound])

trait Inbound

case class InboundDomain(domain: String) extends Inbound

case class Outbound(host: String, port: Int)

trait ApplicationConfig {
  def enabled: Boolean
  def mainClass: String
  def args: Seq[String]
}

case class DependencyAppConfig(enabled: Boolean,
                               group: String,
                               artifact: String,
                               version: String,
                               mainClass: String,
                               args: Seq[String]) extends ApplicationConfig