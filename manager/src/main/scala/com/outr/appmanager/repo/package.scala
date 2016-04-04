package com.outr.appmanager

import scala.language.implicitConversions

package object repo {
  var CurrentScalaVersion: String = "2.11"

  implicit class Group(id: String) {
    def %%(name: String): Dependency = Dependency(id, s"${name}_$CurrentScalaVersion")
    def %(name: String): Dependency = Dependency(id, name)
  }

  implicit def repo2Repo(repository: Repository): coursier.Repository = repository.internal

  implicit def repos2Repos(repos: Seq[Repository]): Seq[coursier.Repository] = repos.map(_.internal)
}