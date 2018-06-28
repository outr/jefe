package com.outr.jefe.launch

import java.io.{BufferedReader, File, InputStreamReader}

import scribe.Logger

import scala.concurrent.Future
import scribe.Execution.global

trait Launcher {
  def launch(): Launched
}

object Launcher {
  private lazy val javaHome = System.getProperty("java.home")
  private lazy val fileSeparator = System.getProperty("file.separator")
  private lazy val pathSeparator = System.getProperty("path.separator")

  lazy val Java: String = {
    val extension = if (fileSeparator == "/") "" else "w.exe"
    s"$javaHome${fileSeparator}bin${fileSeparator}java$extension"
  }
}

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

trait Launched {
  private val loggerId: Long = Logger.empty.replace().id
  def logger: Logger = Logger(loggerId)

  def status: ProcessStatus

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

  private lazy val inputReader = new BufferedReader(new InputStreamReader(process.getInputStream))
  private lazy val errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream))

  Future {
    while (process.isAlive) {
      Option(inputReader.readLine()).foreach(logger.info(_))
    }
  }
  Future {
    while (process.isAlive) {
      Option(errorReader.readLine()).foreach(logger.error(_))
    }
  }

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
    val launcher = new ProcessLauncher(List("./test1.sh"))
    scribe.info("Created launcher!")
    val launched = launcher.launch()
    scribe.info("Launched! Waiting for finished...")
    val finished = launched.waitForFinished()
    scribe.info(s"Finished: $finished")
  }
}