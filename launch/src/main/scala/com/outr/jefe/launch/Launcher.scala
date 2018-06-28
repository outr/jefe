package com.outr.jefe.launch

import java.io.File

trait Launcher {
  def launch(): Launched
}

class ProcessLauncher(processBuilder: ProcessBuilder) extends Launcher {
  override def launch(): Launched = try {
    val process = processBuilder.inheritIO().start()
    LaunchedProcess(process)
  } catch {
    case t: Throwable => FailedProcess(t)
  }
}

trait Launched {
  def status: ProcessStatus

  // TODO: access input

  def waitForFinished(): ProcessStatus = {
    while (status.isRunning) {
      Thread.sleep(10L)
    }
    status
  }

  def stop(force: Boolean): Unit
}

case class FailedProcess(throwable: Throwable) extends Launched {
  override val status: ProcessStatus = ProcessFailedStatus(throwable)

  override def stop(force: Boolean): Unit = {}
}

case class LaunchedProcess(process: Process) extends Launched {
  lazy val processId: Int = {
    val field = process.getClass.getDeclaredFields.find(f => f.getName == "pid" || f.getName == "handle").get
    field.setAccessible(true)
    field.get(process).asInstanceOf[Int]
  }
  lazy val runningStatus: ProcessRunningStatus = ProcessRunningStatus(processId)
  lazy val stoppedStatus: ProcessStoppedStatus = ProcessStoppedStatus(process.exitValue())

  override def status: ProcessStatus = if (process.isAlive) {
    runningStatus
  } else {
    stoppedStatus
  }

  override def stop(force: Boolean): Unit = if (force) {
    process.destroyForcibly()
  } else {
    process.destroy()
  }
}

// TODO: JARLauncher extends ProcessLauncher
// TODO: WARLauncher extends JARLauncher
// TODO: StaticSiteLauncher extends JARLauncher

sealed trait ProcessStatus {
  def isRunning: Boolean
}

case class ProcessFailedStatus(throwable: Throwable) extends ProcessStatus {
  override def isRunning: Boolean = false
}

case class ProcessRunningStatus(processId: Int) extends ProcessStatus {
  override def isRunning: Boolean = true
}

case class ProcessStoppedStatus(exitValue: Int) extends ProcessStatus {
  override def isRunning: Boolean = false
}

object Test {
  // TODO: Migrate to test
  def main(args: Array[String]): Unit = {
    scribe.info(s"Base Directory: ${new File(".").getCanonicalPath}")
    val builder = new ProcessBuilder("./test1.sh")
    val launcher = new ProcessLauncher(builder)
    scribe.info("Created launcher!")
    val launched = launcher.launch()
    scribe.info("Launched! Waiting for finished...")
    val finished = launched.waitForFinished()
    scribe.info(s"Finished: $finished")
  }
}