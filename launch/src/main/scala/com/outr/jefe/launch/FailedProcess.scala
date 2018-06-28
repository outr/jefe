package com.outr.jefe.launch

case class FailedProcess(throwable: Throwable) extends Launched {
  override val status: ProcessStatus = ProcessFailedStatus(throwable)

  override def stop(force: Boolean): Unit = {}
}
