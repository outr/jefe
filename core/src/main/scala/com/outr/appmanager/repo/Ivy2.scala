package com.outr.appmanager.repo

import java.io.File

import org.powerscala.Version

object Ivy2 {
  object Local extends Repository {
    private val baseDirectory = new File(s"${System.getProperty("user.home")}/.ivy2/local")

    override def info(dependency: Dependency): Option[DependencyInfo] = {
      val directory = new File(baseDirectory, s"${dependency.group}/${dependency.name}")
      if (directory.exists()) {
        val directoryNames = directory.listFiles().toList.filter(f => f.isDirectory).map(_.getName)
        val versions = directoryNames.collect {
          case Version(v) => VersionedDependency(dependency, v, Some(this))
        }.sorted.reverse
        val latest = versions.head
        val release = versions.find(!_.version.snapshot)
        Some(DependencyInfo(dependency, latest, release, versions))
      } else {
        None
      }
    }
  }
  object Cache extends Repository {
    private val baseDirectory = new File(s"${System.getProperty("user.home")}/.ivy2/cache")

    override def info(dependency: Dependency): Option[DependencyInfo] = {
      val directory = new File(baseDirectory, s"${dependency.group}/${dependency.name}")
      if (directory.exists()) {
        val xmlNames = directory.listFiles().toList.map(_.getName).filter(s => s.startsWith("ivy-") && s.endsWith(".xml")).map(s => s.substring(4, s.length() - 4))
        val versions = xmlNames.collect {
          case Version(v) => VersionedDependency(dependency, v, Some(this))
        }.sorted.reverse
        val latest = versions.head
        val release = versions.find(!_.version.snapshot)
        Some(DependencyInfo(dependency, latest, release, versions))
      } else {
        None
      }
    }
  }
}