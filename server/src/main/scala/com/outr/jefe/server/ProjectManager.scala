package com.outr.jefe.server

import java.io.File

object ProjectManager {
  private var instances = Map.empty[String, ProjectInstance]

  Runtime.getRuntime.addShutdownHook(new Thread {
    override def run(): Unit = ProjectManager.stop()
  })

  def update(directory: File, configuration: ProjectConfiguration): Unit = synchronized {
    scribe.info("ProjectManager update!")
    val path = directory.getCanonicalPath
    val currentOption = instances.get(path)
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
      instances += path -> instance
      instance.start()
    }
  }

  def stopAllExcept(paths: Set[String]): Unit = {
    scribe.info(s"Stopping all (${instances.keys.mkString(", ")}) except (${paths.mkString(", ")})")
    instances.foreach {
      case (path, instance) => if (!paths.contains(path)) {
        instances -= path
        instance.stop()
      }
    }
  }

  def stop(): Unit = synchronized {
    instances.values.foreach { instance =>
      instance.stop()
    }
    instances = Map.empty
  }
}