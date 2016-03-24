package com.outr.appmanager

package object repo {
  var CurrentScalaVersion: String = "2.11"

  implicit class Group(id: String) {
    def %%(name: String): Dependency = Dependency(id, s"${name}_$CurrentScalaVersion")
    def %(name: String): Dependency = Dependency(id, name)
  }
}