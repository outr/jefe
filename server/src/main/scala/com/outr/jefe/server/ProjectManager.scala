package com.outr.jefe.server

import java.io.File

object ProjectManager {
  private var _instances = Map.empty[String, ProjectInstance]

  def instances: List[ProjectInstance] = _instances.values.toList

  Runtime.getRuntime.addShutdownHook(new Thread {
    override def run(): Unit = ProjectManager.stop()
  })

  def update(directory: File, configuration: ProjectConfiguration): Unit = synchronized {
    scribe.info("ProjectManager update!")
    val path = directory.getCanonicalPath
    val currentOption = _instances.get(path)
    if (currentOption.map(_.configuration).contains(configuration)) {
      scribe.info(s"$path has not changed")
    } else {
      if (currentOption.nonEmpty) {
        scribe.info(s"$path configuration has changed. Reloading...")
        currentOption.foreach(_.stop())
      } else {
        scribe.info(s"$path is loading...")
      }
      val instance = new ProjectInstance(directory, configuration)
      _instances += path -> instance
      instance.start()
    }
  }

  def stopAllExcept(paths: Set[String]): Unit = {
    scribe.info(s"Stopping all (${_instances.keys.mkString(", ")}) except (${paths.mkString(", ")})")
    _instances.foreach {
      case (path, instance) => if (!paths.contains(path)) {
        _instances -= path
        instance.stop()
      }
    }
  }

  def stop(): Unit = synchronized {
    _instances.values.foreach { instance =>
      instance.stop()
    }
    _instances = Map.empty
  }
}