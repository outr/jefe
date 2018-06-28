package com.outr.jefe.launch

import java.io.{BufferedReader, InputStreamReader}

import scala.concurrent.Future
import scribe.Execution.global

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
