package com.outr.jefe.launch

import com.outr.reactify.{StateChannel, Var}

trait LauncherInstance {
  protected val _status: Var[LauncherStatus] = Var[LauncherStatus](LauncherStatus.NotStarted)
  protected val _error: Var[Option[Throwable]] = Var[Option[Throwable]](None)

  val status: StateChannel[LauncherStatus] = _status
  val error: StateChannel[Option[Throwable]] = _error

  def start(): Unit

  def stop(): Unit
}