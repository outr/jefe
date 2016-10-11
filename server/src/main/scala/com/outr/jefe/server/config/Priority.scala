package com.outr.jefe.server.config

object Priority {
  private var map = Map.empty[String, Priority]

  case object Lowest extends Priority(1, "lowest")
  case object Low extends Priority(2, "low")
  case object Normal extends Priority(3, "normal")
  case object High extends Priority(4, "high")
  case object Critical extends Priority(5, "critical")

  def get(name: String): Option[Priority] = map.get(name)
}

sealed abstract class Priority(val value: Int, val name: String) {
  Priority.map += name -> this
}