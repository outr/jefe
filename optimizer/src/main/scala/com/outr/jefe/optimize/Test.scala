package com.outr.jefe.optimize

import java.io.File
import java.util
import java.util.jar.JarFile

import com.outr.scribe.{Logger, Logging}
import org.objectweb.asm._

import scala.collection.JavaConversions._

object Test extends Logging {
  def main(args: Array[String]): Unit = {
    val jar = new JarFile("../runner.jar")
    val inspector = new ApplicationInspector(jar)
    inspector.process("com/outr/jefe/runner/Runner$")
    val count = jar.entries().collect {
      case e if e.getName.endsWith(".class") => e
    }.size
    logger.info(s"Total classes: $count")
    val o = new Optimizer("com.outr.jefe.runner.Runner$", new File("../runner.jar"), new File("../runner-optimized.jar"), new File("../classes.list"), new File("../wildcards.list"))
    o.classes = inspector.referenced.map(s => s"${s.replace('/', '.')}.class")
    o.optimize(run = false)
  }
}

class Sample {
  val list = new util.ArrayList[String]()

  def test(): Unit = {
    Logger.root.info("Sample sample!")
    simple.test()
  }

  object simple {
    def test(): Unit = println("Blah!")
  }
}