package com.outr.appmanager.repo

trait Repositories extends Seq[Repository] {
  def add(repository: Repository): Unit
}
