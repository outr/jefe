package com.outr.jefe.pack

import java.io.File
import java.net.URL

import com.badlogicgames.packr.Packr
import com.badlogicgames.packr.Packr.{Config, Platform}
import com.outr.jefe.optimize.Optimizer
import org.powerscala.io._
import proguard.{Configuration, ConfigurationParser, ProGuard}

import scala.collection.JavaConversions._

object Pack extends App {
  object JRE {
    def load(url: String): File = {
      val file = new File(outputDir, url.substring(url.lastIndexOf('/') + 1))
      if (!file.exists()) {
        IO.stream(new URL(url), file)
      }
      file
    }

    lazy val Windows = load("http://cdn.azul.com/zulu/bin/zulu8.13.0.5-jdk8.0.72-win_x64.zip")
    lazy val Mac = load("http://cdn.azul.com/zulu/bin/zulu8.13.0.5-jdk8.0.72-macosx_x64.zip")
    lazy val Linux = load("http://cdn.azul.com/zulu/bin/zulu8.13.0.5-jdk8.0.72-linux_x64.tar.gz")
  }

  val outputDir = new File("../output")
  outputDir.mkdirs()
  val assemblyJAR = new File("../runner/target/scala-2.11/jefe-runner-assembly-1.0.0.jar")
  var jar = new File(outputDir, "runner.jar")

//  simpleOptimize()
  optimize()
//  pack()

  def simpleOptimize(): Unit = {
    jar.delete()

    val classList = new File(outputDir, "includes.list")
    val wildCards = new File(outputDir, "wildcards.list")
    val optimizer = new Optimizer("com.outr.jefe.runner.Runner", assemblyJAR, jar, classList, wildCards)
    optimizer.optimize()
  }

  def optimize(): Unit = {
    jar.delete()

    val config =
      s"""
        |-injars      ${assemblyJAR.getCanonicalPath}
        |-outjars     ${jar.getCanonicalPath}
        |-libraryjars <java.home>/lib/rt.jar
        |-libraryjars <java.home>/lib/jce.jar
        |
        |-dontwarn scala.**
        |
        |-keepclasseswithmembers public class * {
        |    public static void main(java.lang.String[]);
        |}
        |
        |-keep class * implements org.xml.sax.EntityResolver
        |
        |-keepclassmembers class * {
        |    ** MODULE$$;
        |}
        |
        |-keepclassmembernames class scala.concurrent.forkjoin.ForkJoinPool {
        |    long eventCount;
        |    int  workerCounts;
        |    int  runControl;
        |    scala.concurrent.forkjoin.ForkJoinPool$$WaitQueueNode syncStack;
        |    scala.concurrent.forkjoin.ForkJoinPool$$WaitQueueNode spareStack;
        |}
        |
        |-keepclassmembernames class scala.concurrent.forkjoin.ForkJoinWorkerThread {
        |    int base;
        |    int sp;
        |    int runState;
        |}
        |
        |-keepclassmembernames class scala.concurrent.forkjoin.ForkJoinTask {
        |    int status;
        |}
        |
        |-keepclassmembernames class scala.concurrent.forkjoin.LinkedTransferQueue {
        |    scala.concurrent.forkjoin.LinkedTransferQueue$$PaddedAtomicReference head;
        |    scala.concurrent.forkjoin.LinkedTransferQueue$$PaddedAtomicReference tail;
        |    scala.concurrent.forkjoin.LinkedTransferQueue$$PaddedAtomicReference cleanMe;
        |}
        |
        |-dontoptimize
        |-dontobfuscate
        |-ignorewarnings
        |
        |-verbose
      """.stripMargin

    val configuration = new Configuration
    val parser = new ConfigurationParser(Array(config), System.getProperties)
    parser.parse(configuration)
    parser.close()
    val pg = new ProGuard(configuration)
    pg.execute()
  }

  def pack(): Unit = {
    val config = new Config {
      executable = "runner"
      classpath = List(jar.getCanonicalPath)
      mainClass = "com.outr.jefe.runner.Runner"
      vmArgs = Nil
      minimizeJre = Array(
        "jre/lib/rt/com/sun/corba",
        "jre/lib/rt/com/sun/jmx",
        "jre/lib/rt/com/sun/jndi",
        "jre/lib/rt/com/sun/media",
        "jre/lib/rt/com/sun/naming",
        "jre/lib/rt/com/sun/org",
        "jre/lib/rt/com/sun/rowset",
        "jre/lib/rt/com/sun/script",
        "jre/lib/rt/com/sun/xml",
        "jre/lib/rt/sun/applet",
        "jre/lib/rt/sun/corba",
        "jre/lib/rt/sun/management"
      )
      resources.add("../config")
    }

    def runFor(jdk: File, platform: Platform): Unit = {
      config.platform = platform
      config.jdk = jdk.getAbsolutePath
      config.outDir = new File(outputDir, platform.name()).getAbsolutePath
      new Packr().pack(config)
    }

    runFor(JRE.Windows, Platform.windows64)
    runFor(JRE.Mac, Platform.mac)
  }
}
