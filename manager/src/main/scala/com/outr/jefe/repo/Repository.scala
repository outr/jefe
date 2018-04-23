package com.outr.jefe.repo

import sbt.librarymanagement.Resolver
import coursier.{Repository => CoursierRepository}

trait Repository {
  def info(dependency: Dependency): Option[DependencyInfo]

  def sbt: Resolver
  def coursier: CoursierRepository
}

object Repository {
  def info(dependency: Dependency, repositories: Seq[Repository]): Option[DependencyInfo] = {
    val infos = repositories.flatMap(_.info(dependency))
    if (infos.nonEmpty) {
      var info = infos.head
      infos.tail.foreach { i =>
        info = info.merge(i)
      }
      Some(info)
    } else {
      None
    }
  }
}