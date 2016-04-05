package com.outr.jefe.repo

trait Repository {
  def info(dependency: Dependency): Option[DependencyInfo]

  def internal: coursier.Repository
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