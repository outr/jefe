package com.outr.jefe

import scala.language.implicitConversions

package object repo {
  var CurrentScalaVersion: String = "2.12"

  implicit class Group(id: String) {
    def %%(name: String): Dependency = Dependency(id, s"${name}_$CurrentScalaVersion")
    def %(name: String): Dependency = Dependency(id, name)
  }
}