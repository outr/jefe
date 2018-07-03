package com.outr.jefe.launch

import java.io.{BufferedReader, InputStreamReader}

import com.outr.jefe.launch.JMXProcessMonitor.ProcessStats

import scala.concurrent.Future
import scribe.Execution.global

case class LaunchedProcess(launcher: Launcher, process: Process) extends Launched {
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

  override def stats(): Option[ProcessStats] = if (process.isAlive) {
    launcher match {
      case jar: JARLauncher => jar.jmxConfig.map(JMXProcessMonitor.stats)
      case _ => None
    }
  } else {
    None
  }

  override def stop(force: Boolean): Unit = if (force) {
    process.destroyForcibly()
  } else {
    process.destroy()
  }
}
