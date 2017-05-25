package com.outr.jefe.server.config

import java.io.File

import com.outr.jefe.runner.Repositories

class WARAppConfig(enabled: Boolean,
                   war: File,
                   port: Int,
                   jmxPort: Int,
                   vmArgs: Seq[String],
                   repositories: Repositories = Repositories.simple()) extends DependencyAppConfig(
  enabled,
  war.getParentFile,
  "org.eclipse.jetty",
  "jetty-runner",
  "9.4.5.v20170502",
  "org.eclipse.jetty.runner.Runner",
  List("--port", port.toString, war.getCanonicalPath),
  jmxPort,
  vmArgs,
  repositories,
  scalaVersion = None
)