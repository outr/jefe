package com.outr.jefe.launch

import java.io.File

import scala.collection.mutable.ListBuffer

class JARLauncher(val jars: List[File],
                  val mainClass: Option[String] = None,
                  jvmArgs: List[String] = Nil,        // TODO: support typed entries to replace String
                  args: List[String] = Nil,           // TODO: support typed entries to replace String
                  jmxConfig: Option[JMXConfig] = None,
                  workingDirectory: File = new File("."),
                  environment: Map[String, String] = Map.empty)
  extends ProcessLauncher(
    commands = JARLauncher.buildCommands(jars, mainClass, jvmArgs, args, jmxConfig),
    workingDirectory = workingDirectory,
    environment = environment
  )

object JARLauncher {
  private lazy val javaHome = System.getProperty("java.home")
  private lazy val fileSeparator = System.getProperty("file.separator")

  lazy val Java: String = {
    val extension = if (fileSeparator == "/") "" else "w.exe"
    s"$javaHome${fileSeparator}bin${fileSeparator}java$extension"
  }

  private def buildCommands(jars: List[File],
                            mainClass: Option[String],
                            jvmArgs: List[String],
                            args: List[String],
                            jmxConfig: Option[JMXConfig]): List[String] = {
    val b = ListBuffer.empty[String]
    b += Java
    jmxConfig.foreach(c => b ++= c.args)
    b ++= jvmArgs
    b += "-cp"
    b ++= jars.map(_.getAbsolutePath)
    mainClass.foreach(b += _)
    b ++= args
    b.toList
  }
}