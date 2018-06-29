package com.outr.jefe.launch

import java.io.File

class ProcessLauncher(val commands: List[String],
                      val workingDirectory: File = new File("."),
                      val environment: Map[String, String] = Map.empty) extends Launcher {
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
    val process = processBuilder.start()
    LaunchedProcess(process)
  } catch {
    case t: Throwable => FailedProcess(t)
  }
}