package com.outr.jefe.launch

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