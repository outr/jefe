package com.outr.appmanager.launch

import pl.metastack.metarx.{StateChannel, Var}

trait LauncherInstance {
  protected val _status = Var[LauncherStatus](LauncherStatus.NotStarted)
  protected val _error = Var[Option[Throwable]](None)

  val status: StateChannel[LauncherStatus] = _status
  val error: StateChannel[Option[Throwable]] = _error

  def start(): Unit
}