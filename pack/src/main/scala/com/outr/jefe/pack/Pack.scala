package com.outr.jefe.pack

import java.io.File
import java.net.URL

import com.badlogicgames.packr.Packr
import com.badlogicgames.packr.Packr.{Config, Platform}
import org.powerscala.io._
import proguard.{ClassPathEntry, Configuration, ConfigurationParser, ProGuard}

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

    object Windows {
      lazy val i586: File = load("https://bitbucket.org/alexkasko/openjdk-unofficial-builds/downloads/openjdk-1.7.0-u80-unofficial-windows-i586-image.zip")
      lazy val amd64: File = load("https://bitbucket.org/alexkasko/openjdk-unofficial-builds/downloads/openjdk-1.7.0-u80-unofficial-windows-amd64-image.zip")
    }
    object Mac {
      lazy val x86_64: File = load("https://bitbucket.org/alexkasko/openjdk-unofficial-builds/downloads/openjdk-1.7.0-u80-unofficial-macosx-x86_64-image.zip")
    }
    object Linux {
      lazy val i586: File = load("https://bitbucket.org/alexkasko/openjdk-unofficial-builds/downloads/openjdk-1.7.0-u80-unofficial-linux-i586-image.zip")
      lazy val amd64: File = load("https://bitbucket.org/alexkasko/openjdk-unofficial-builds/downloads/openjdk-1.7.0-u80-unofficial-linux-amd64-image.zip")
    }
  }

  val outputDir = new File("../output")
  outputDir.mkdirs()
  val assemblyJAR = new File("../runner/target/scala-2.11/jefe-runner-assembly-1.0.0.jar")
  var jar = new File(outputDir, "runner.jar")

  optimize()
  pack()

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
        |
        |-dontobfuscate
        |-dontshrink
        |-dontpreverify
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

    runFor(JRE.Windows.amd64, Platform.windows64)
    runFor(JRE.Windows.i586, Platform.windows32)
    runFor(JRE.Mac.x86_64, Platform.mac)
    runFor(JRE.Linux.amd64, Platform.linux64)
    runFor(JRE.Linux.i586, Platform.linux32)
  }
}
