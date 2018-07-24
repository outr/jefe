package com.outr.jefe.application

import reactify.{Val, Var}
import reactify.instance.RecursionMode

object ApplicationManager {
  private val _applications: Var[List[Application]] = Var(Nil, recursion = RecursionMode.None)

  def all: Val[List[Application]] = _applications

  def +=(application: Application): Application = synchronized {
    this -= application
    _applications.static(application :: _applications())
    application
  }

  def -=(application: Application): Application = synchronized {
    application.stop(force = false)
    _applications.static(_applications().filterNot(_.id == application.id))
    application
  }

  def launch(application: Application): Application = {
    this += application
    application.start()
    application
  }

  def byId(id: String): Option[Application] = all().find(_.id == id)
}