package com.outr.appmanager.repo

trait Repository {
  def info(dependency: Dependency): Option[DependencyInfo]
}
