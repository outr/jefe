package com.outr.jefe.launch

import java.io.File

import scala.collection.mutable.ListBuffer
import scala.reflect.internal.util.ScalaClassLoader.URLClassLoader

class Launcher(mainClass: String,
               files: Seq[File],
               args: Seq[String] = Nil) {
  private lazy val javaHome = System.getProperty("java.home")
  private lazy val fileSeparator = System.getProperty("file.separator")
  private lazy val pathSeparator = System.getProperty("path.separator")

  def classLoaded(parentClassLoader: Option[ClassLoader] = None): ClassLoaderLauncherInstance = {
    val classLoader = new URLClassLoader(Array(files.map(_.toURI.toURL): _*), parentClassLoader.orNull)
    Thread.currentThread().setContextClassLoader(classLoader)
    val clazz = classLoader.loadClass(mainClass)
    val main = clazz.getMethod("main", classOf[Array[String]])
    new ClassLoaderLauncherInstance(classLoader, () => main.invoke(null, args.toArray))
  }

  def process(workingDirectory: File, vmArgs: String*): ProcessLauncherInstance = {
    val extension = if (fileSeparator == "/") "" else "w.exe"
    val javaPath = s"$javaHome${fileSeparator}bin${fileSeparator}java$extension"
    val b = ListBuffer.empty[String]
    b += javaPath
    vmArgs.foreach { arg =>
      b += arg
    }
    b += "-cp"
    b += (List(".") ::: files.toList.map(_.getAbsolutePath)).mkString(pathSeparator)
    b += mainClass
    args.foreach { arg =>
      b += arg
    }
    val builder = new ProcessBuilder(b: _*)
    builder.directory(workingDirectory)
    new ProcessLauncherInstance(builder)
  }
}