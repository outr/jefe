package com.outr.jefe.application

trait Application {
  def id: String
  def start(): Unit
  def isRunning: Boolean
  def restart(force: Boolean): Unit
  def stop(force: Boolean): Unit
}