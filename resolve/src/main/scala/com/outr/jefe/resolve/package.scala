package com.outr.jefe


import scala.language.implicitConversions

package object resolve {
  var CurrentScalaVersion: String = "2.12"

  implicit class Group(groupId: String) {
    def %%(name: String): Artifact = Artifact(groupId, s"${name}_$CurrentScalaVersion")
    def %(name: String): Artifact = Artifact(groupId, name)
  }

  implicit def string2Version(version: String): Version = Version(version)
}