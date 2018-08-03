package com.outr.jefe.boot.command

trait Command {
  def name: String
  def description: String

  def execute(): Unit
  def help(): Unit
}