package com.outr.appmanager.repo

import java.io.File

import com.outr.scribe.formatter.FormatterBuilder
import com.outr.scribe.writer.FileWriter
import com.outr.scribe.{LogHandler, Logger, Logging}
import org.powerscala.Version
import org.powerscala.io.IO

import scala.annotation.tailrec
import scala.collection.mutable

class DependencyResolver(vd: VersionedDependency, initialRepos: List[Repository]) extends Repositories with Logging {
  private var repositories: List[Repository] = initialRepos

  override def add(repository: Repository): Unit = synchronized {
    repositories = (repository :: repositories).distinct
  }

  override def length: Int = repositories.length

  override def apply(idx: Int): Repository = repositories(idx)

  override def iterator: Iterator[Repository] = repositories.iterator

  private val depLog = new Logger("dependencies", parent = None)
  depLog.addHandler(
    LogHandler(
      formatter = FormatterBuilder().message.newLine,
      writer = new FileWriter(
        new File("logs"),
        () => s"dependencies.log",
        append = false,
        autoFlush = true
      )
    )
  )

  private var _dependencies = Map.empty[String, VersionedDependency]
  private val backlog = mutable.Queue.empty[VersionedDependency]
  private var scopes = Set.empty[String]

  def dependencies: Map[String, VersionedDependency] = _dependencies
  def latest: List[VersionedDependency] = {
    var map = Map.empty[String, VersionedDependency]
    dependencies.values.foreach { vd =>
      map.get(vd.dependency.toString) match {
        case Some(other) if other.version >= vd.version => // Ignore if the version of other is <= current version
        case _ => map += vd.dependency.toString -> vd
      }
    }
    map.values.toList
  }

  def versionForGroupId(groupId: String): Option[Version] = _dependencies.values.collect {
    case dep if dep.group == groupId => dep.version
  }.toList.sorted.lastOption.orElse(backlog.collect {
    case dep if dep.group == groupId => dep.version
  }.toList.sorted.lastOption)

  def download(directory: File, latestOnly: Boolean = true, useCache: Boolean = true): Unit = {
    val deps = if (latestOnly) latest else dependencies.values.toList
    deps.foreach { d =>
      val filename = s"${d.name}-${d.version}.jar"
      val file = new File(directory, filename)
      if (useCache && file.exists()) {
        logger.info(s"$filename already cached.")
        // Already cached, don't re-download
      } else {
        logger.info(s"$filename downloading from ${d.repository.get}...")
        IO.stream(d.jar, file)
      }
    }
  }

  def resolve(): Unit = {
    DependencyResolver.instance.set(this)
    try {
      depLog.info(s"Resolving Dependencies for $vd...")
      _dependencies = Map.empty
      backlog.clear()

      _dependencies += vd.string -> vd
      backlog ++= vd.dependencies.filter(_.scope.isEmpty)
      resolveDependencies()
    } finally {
      DependencyResolver.instance.remove()
    }
  }

  @tailrec
  private def resolveDependencies(): Unit = if (backlog.isEmpty) {
    // Empty
  } else {
    resolveDependency(backlog.dequeue())

    resolveDependencies()
  }

  private def resolveDependency(dep: VersionedDependency): Unit = {
    if (!_dependencies.contains(dep.string)) {       // Make sure we're not processing a second time
      val updated = dep.resolve(this)
      _dependencies += updated.string -> updated

      // TODO: allow VersionedDependency with 0.0 version to be replaced before adding
      println(s"Dep: $updated, Parent: ${updated.parent}")
      // Check for parent
      updated.parent.foreach { p =>
        resolveDependency(p)
      }

      val deps = updated.dependencies.filter(_.scope.isEmpty)
      val filtered = updated.dependencies.filterNot(_.scope.isEmpty)
      filtered.foreach { d =>
        scopes += d.scope.get
      }
      logger.info(s"Adding ${updated.string} to list with dependencies: [${deps.mkString(", ")}]")
      depLog.info("")
      depLog.info(s"$updated (${updated.repository.get})")
      deps.foreach(d => depLog.info(s"+ $d"))
      filtered.foreach(d => depLog.info(s"- $d"))

      backlog ++= deps
    } else {
      logger.debug(s"Ignoring ${dep.string}...")
    }
  }
}

object DependencyResolver {
  private val instance = new ThreadLocal[DependencyResolver]

  def get(): Option[DependencyResolver] = Option(instance.get())
}