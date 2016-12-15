package com.outr.jefe.server.config

import java.io.File

import com.outr.jefe.runner.Repositories

class WARAppConfig(enabled: Boolean, war: File, port: Int, jmxPort: Int, vmArgs: Seq[String]) extends DependencyAppConfig(
  enabled,
  war.getParentFile,
  "org.eclipse.jetty",
  "jetty-runner",
  "9.3.9.v20160517",
  "org.eclipse.jetty.runner.Runner",
  List("--port", port.toString, war.getCanonicalPath),
  jmxPort,
  vmArgs,
  Repositories(),
  scala = false
)
