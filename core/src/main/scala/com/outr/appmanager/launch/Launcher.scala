package com.outr.appmanager.launch

import java.io.File

import scala.collection.mutable.ListBuffer
import scala.reflect.internal.util.ScalaClassLoader.URLClassLoader

class Launcher(mainClass: String,
               files: Seq[File],
               args: Seq[String] = Nil) {
  private lazy val fileSeparator = System.getProperty("file.separator")
  private lazy val pathSeparator = System.getProperty("path.separator")

  def classLoaded(parentClassLoader: Option[ClassLoader] = None): ClassLoaderLaunched = {
    val classLoader = new URLClassLoader(Array(files.map(_.toURI.toURL): _*), parentClassLoader.orNull)
    Thread.currentThread().setContextClassLoader(classLoader)
    val clazz = classLoader.loadClass(mainClass)
    val main = clazz.getMethod("main", classOf[Array[String]])
    new ClassLoaderLaunched(classLoader, () => main.invoke(null, args.toArray))
  }

  def process(vmArgs: String*): Launched = {
    val extension = if (fileSeparator == "/") "" else "w.exe"
    val javaPath = s"${System.getProperty("java.home")}${fileSeparator}bin${fileSeparator}java$extension"
    val b = ListBuffer.empty[String]
    b += javaPath
    vmArgs.foreach { arg =>
      b += arg
    }
    b += "-cp"
    b += files.map(_.getAbsolutePath).mkString(pathSeparator)
    b += mainClass
    args.foreach { arg =>
      b += arg
    }
    val builder = new ProcessBuilder(b: _*).inheritIO()
    new ProcessLaunched(builder)
  }
}

sealed trait Launched {
  // TODO: status should be a Var
  def status: LauncherStatus
  def error: Option[Throwable]
}

class ProcessLaunched(builder: ProcessBuilder) extends Launched {
  // TODO: properly support status and error
  private var _status: LauncherStatus = LauncherStatus.NotStarted
  private var _error: Option[Throwable] = None

  override def status: LauncherStatus = _status
  override def error: Option[Throwable] = _error

  val process = builder.start()
}

class ClassLoaderLaunched(classLoader: ClassLoader, runner: () => Unit) extends Launched {
  private var _status: LauncherStatus = LauncherStatus.NotStarted
  private var _error: Option[Throwable] = None

  override def status: LauncherStatus = _status
  override def error: Option[Throwable] = _error

  val runnable = new Runnable {
    override def run(): Unit = {
      _status = LauncherStatus.Running
      try {
        runner()
      } catch {
        case t: Throwable => _error = Some(t)
      } finally {
        _status = LauncherStatus.Finished
      }
    }
  }
  val thread = new Thread(runnable)

  _status = LauncherStatus.Starting
  thread.start()
}

sealed trait LauncherStatus

object LauncherStatus {
  case object NotStarted extends LauncherStatus
  case object Starting extends LauncherStatus
  case object Running extends LauncherStatus
  case object Finished extends LauncherStatus
}