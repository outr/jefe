package com.outr.jefe.server

case class ApplicationConfiguration(`type`: String,
                                    enabled: Option[Boolean],
                                    group: Option[String],
                                    artifact: Option[String],
                                    version: Option[String],
                                    mainClass: Option[String],
                                    scala: Option[Boolean],
                                    scalaVersion: Option[String],
                                    basePath: Option[String],
                                    args: Option[List[String]],
                                    vmArgs: Option[List[String]],
                                    ivyLocal: Option[Boolean],
                                    mavenRepositories: Option[Map[String, String]],
                                    host: Option[String],
                                    port: Option[Int],
                                    jar: Option[String],
                                    war: Option[String])
