package com.outr.jefe.runner

import java.awt.GraphicsEnvironment
import java.io.File

import com.outr.jefe.launch.{Launcher, LauncherInstance, LauncherStatus}
import com.outr.jefe.repo._
import com.outr.scribe.Logging

class Arguments(args: Array[String]) {
  private var entries = args.toList

  def containsAll(keys: String*): Boolean = keys.forall(contains)

  def contains(key: String): Boolean = entries.exists(_.startsWith(s"$key="))

  def takeOrElse(key: String, default: => String): String = entries.find(_.startsWith(s"$key=")) match {
    case Some(value) => {
      entries = entries.filterNot(_ == value)
      value.substring(key.length + 1)
    }
    case None => default
  }

  def take(key: String): String = takeOrElse(key, throw new RuntimeException(s"Unable to find $key in ${args.mkString(", ")}"))

  def leftOvers: Seq[String] = entries
}

object Runner extends Logging {
  def main(args: Array[String]): Unit = {
    val a = new Arguments(args)
    val configuration: Option[Configuration] = if (!a.containsAll("groupId", "artifactId", "mainClass")) {
      val configFile = new File(a.takeOrElse("config", "config"))
      if (configFile.exists()) {
        Option(Configuration.load(configFile))
      } else {
        fail("Usage: java -jar runner.jar groupId=com.company artifactId=project (version=1.0.0|latest) mainClass=com.company.MyClass arguments")
        None
      }
    } else {
      val groupId = a.take("groupId")
      val artifactId = a.take("artifactId")
      val mainClass = a.take("mainClass")
      val version = a.takeOrElse("version", "latest")
      Some(Configuration(groupId %% artifactId % version, mainClass, a.leftOvers.toArray))
    }
    configuration.foreach(run(_))
  }

  def run(configuration: Configuration): LauncherInstance = {
    start(configuration)
  }

  private def fail(message: String): Unit = {
    System.err.println(message)
    System.exit(1)
  }

  def start(configuration: Configuration): LauncherInstance = {
    val monitor = if (!configuration.showDialogIfPossible || GraphicsEnvironment.isHeadless) {
      Monitor.Console
    } else {
      Monitor.Dialog
    }
    logger.info("Initializing dependency manager...")
    val manager = DependencyManager(configuration.repositories.list, monitor)
    logger.info("Resolving dependencies...")
    val files = manager.resolve(configuration.dependency)
    logger.info("Creating launcher...")
    val launcher = new Launcher(configuration.mainClass, files, configuration.args)
    logger.info("Creating launcher instance...")
    val instance = launcher.classLoaded()
    logger.info("Starting application...")
    instance.start()
    instance
  }

  def waitFor(instance: LauncherInstance): Unit = {
    while (instance.status.get != LauncherStatus.Finished) {
      Thread.sleep(50)
    }
  }
}