package com.outr.appmanager.repo

import java.io.File

import org.powerscala.Version

case class Ivy2(baseDirectory: File) extends Repository {
  override def info(dependency: Dependency): Option[DependencyInfo] = {
    val directory = new File(baseDirectory, s"${dependency.group}/${dependency.name}")
    val directoryNames = directory.listFiles().toList.filter(f => f.isDirectory).map(_.getName)
    val versions = directoryNames.collect {
      case Version(v) => v
    }.sorted
    println(s"Versions: ${versions.map(v => s"$v (${v.snapshot})").mkString(", ")}")
    None
  }
}

object Ivy2 {
  val Local = Ivy2(new File(s"${System.getProperty("user.home")}/.ivy2/local"))
  val Cache = Ivy2(new File(s"${System.getProperty("user.home")}/.ivy2/cache"))
}