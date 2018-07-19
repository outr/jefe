package com.outr.jefe.launch

import java.io.File

import scribe.Logger

class ProcessLauncher(val commands: List[String],
                      val workingDirectory: File = new File("."),
                      val environment: Map[String, String] = Map.empty,
                      val loggerId: Long = Logger.root.id) extends Launcher {
  private lazy val processBuilder = {
    val b = new ProcessBuilder(commands: _*)
    b.directory(workingDirectory)
    val env = b.environment()
    environment.foreach {
      case (key, value) => env.put(key, value)
    }
    b
  }

  override def launch(): Launched = try {
    scribe.info(s"Launching: ${commands.mkString(" ")}")
    val process = processBuilder.start()
    LaunchedProcess(this, process)
  } catch {
    case t: Throwable => FailedProcess(this, t)
  }
}