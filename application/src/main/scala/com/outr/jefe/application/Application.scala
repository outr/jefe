package com.outr.jefe.application

trait Application {
  def start(): Unit
  def isRunning: Boolean
  def restart(force: Boolean): Unit
  def stop(force: Boolean): Unit
}