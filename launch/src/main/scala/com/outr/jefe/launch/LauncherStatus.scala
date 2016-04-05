package com.outr.jefe.launch

sealed trait LauncherStatus

object LauncherStatus {
  case object NotStarted extends LauncherStatus
  case object Starting extends LauncherStatus
  case object Running extends LauncherStatus
  case object Finished extends LauncherStatus
}