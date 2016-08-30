package com.outr.jefe.optimize

import java.io.{File, FileOutputStream}
import java.net.URLClassLoader
import java.util.zip.{ZipEntry, ZipFile, ZipOutputStream}

import com.outr.scribe.Logging
import org.powerscala.StringUtil
import org.powerscala.io.IO

import scala.annotation.tailrec
import scala.collection.JavaConversions._

class Optimizer(mainClass: String, inJAR: File, outJAR: File, classList: File, wildcardsList: File) extends URLClassLoader(Array(inJAR.toURI.toURL), null) with Logging {
  var classes = Set.empty[String]
  private val wildcards = if (wildcardsList.exists()) {
    IO.stream(wildcardsList, new StringBuilder).toString.split("\n").map(_.trim).toList
  } else {
    Nil
  }
  private val ignores = Set("java.", "javax.", "com.sun.", "org.xml.sax.", "sun.")

  private var include = 0
  private var exclude = 0

  override def loadClass(name: String, resolve: Boolean): Class[_] = {
    logger.debug(s"loadClass(name = $name, resolve = $resolve)")
    synchronized {
      if (ignores.exists(name.startsWith)) {
        // Ignore Java built-in classes
      } else {
        classes += s"$name.class"
      }
    }
    super.loadClass(name, resolve)
  }

  def optimize(run: Boolean): Unit = {
    if (classList.exists()) {
      classes = IO.stream(classList, new StringBuilder).toString.split("\n").map(_.trim).toSet
    }

    if (run) {
      Thread.currentThread().setContextClassLoader(this)
      val clazz = loadClass(mainClass)
      val main = clazz.getMethod("main", classOf[Array[String]])
      val instance = main.invoke(null, Array.empty[String])
      val waitFor = clazz.getMethod("waitFor")
      waitFor.invoke(instance)
      logger.info(s"Finished. Found ${classes.size} classes.")
      IO.stream(classes.mkString("\n"), classList)
    }

    val in = new ZipFile(inJAR)
    val entries = in.entries().toList
    val out = new ZipOutputStream(new FileOutputStream(outJAR))
    try {
      write(in, entries, out)
    } finally {
      in.close()
      out.flush()
      out.close()
    }
    logger.info(s"Include: $include, Exclude: $exclude, Original Size: ${StringUtil.humanReadableByteCount(inJAR.length())}, Optimized Size: ${StringUtil.humanReadableByteCount(outJAR.length())}, Trimmed: ${StringUtil.humanReadableByteCount(inJAR.length() - outJAR.length())}")
    if (classes.nonEmpty) {
      logger.info(s"Couldn't find (${classes.size} classes): ${classes.mkString(", ")}")
    }
  }

  private def includeClass(className: String): Boolean = {
    classes.contains(className) || wildcards.exists(className.matches)
  }

  @tailrec
  private def write(in: ZipFile, entries: List[ZipEntry], out: ZipOutputStream): Unit = entries.headOption match {
    case None => // Finished
    case Some(entry) => {
      def writeEntry(): Unit = {
        val e = new ZipEntry(entry)
        out.putNextEntry(e)
        val input = in.getInputStream(entry)
        IO.stream(input, out, closeOnComplete = false)
        out.closeEntry()
      }

      val fullName = entry.getName.replace('/', '.')
      if (fullName.endsWith(".class")) {
        if (includeClass(fullName)) {
          classes -= fullName
          include += 1

          writeEntry()
        } else {
          logger.info(s"Excluding: $fullName")
          exclude += 1
        }
      } else {
        writeEntry()
      }
      logger.debug(s"Entry: $fullName")
      write(in, entries.tail, out)
    }
  }
}

object Optimizer extends App {
  if (args.length < 2) exitWithError("usage: optimizer <mainClass> <input JAR> (output JAR) (classlist file [includes.list]) (wildcards file [wildcards.list])")

  val mainClass = args(0)
  val inputJAR = new File(args(1))
  if (!inputJAR.exists()) exitWithError(s"The input JAR does not exist: ${inputJAR.getAbsolutePath}")
  val outputJAR = new File(argOrElse(2, {
    val path = inputJAR.getAbsolutePath
    s"${path.substring(0, path.length - 4)}-optimized.jar"
  }))
  val classListFile = new File(argOrElse(3, "includes.list"))
  val wildcardsFile = new File(argOrElse(4, "wildcards.list"))

  val optimizer = new Optimizer(mainClass, inputJAR, outputJAR, classListFile, wildcardsFile)
  optimizer.optimize(run = true)

  private def argOrElse(index: Int, default: String): String = if (args.length > index) {
    args(index)
  } else {
    default
  }

  private def exitWithError(message: String): Unit = {
    System.err.println(message)
    System.exit(1)
  }
}