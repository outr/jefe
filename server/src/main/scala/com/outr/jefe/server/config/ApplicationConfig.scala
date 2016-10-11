package com.outr.jefe.server.config

trait ApplicationConfig {
  def start(): Unit

  def stop(): Unit

  def enabled: Boolean
}