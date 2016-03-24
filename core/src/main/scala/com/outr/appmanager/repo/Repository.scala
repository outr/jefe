package com.outr.appmanager.repo

import java.net.URL

trait Repository {
  def info(dependency: Dependency): Option[DependencyInfo]

  def jarFor(dependency: VersionedDependency): URL

  def dependenciesFor(dependency: VersionedDependency): List[VersionedDependency]
}

object Repository {
  def info(dependency: Dependency, repositories: Repository*): Option[DependencyInfo] = {
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