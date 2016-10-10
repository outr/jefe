package com.outr.jefe.optimize

import java.util.jar.JarFile

import com.outr.scribe.Logging
import org.objectweb.asm._

import scala.annotation.tailrec
import scala.collection.mutable

class ApplicationInspector(jar: JarFile) extends Logging {
  var referenced = Set.empty[String]
  private val backlog = mutable.Queue.empty[String]

  private val exclusions = Set("java/", "javax/", "org/xml/sax/", "sun/")

  val mv = new MethodVisitor(Opcodes.ASM5) {
    override def visitFieldInsn(opcode: Int, owner: String, name: String, desc: String): Unit = {
      enqueue(owner)
    }

    override def visitMethodInsn(opcode: Int, owner: String, name: String, desc: String, itf: Boolean): Unit = {
      enqueue(owner)
    }
  }
  private val cv = new ClassVisitor(Opcodes.ASM5) {
    override def visitField(access: Int, name: String, desc: String, signature: String, value: scala.Any): FieldVisitor = {
      if (desc.startsWith("L")) {
        val className = desc.substring(1, desc.length - 1)
        enqueue(className)
      }
      super.visitField(access, name, desc, signature, value)
    }

    override def visitMethod(access: Int, name: String, desc: String, signature: String, exceptions: Array[String]): MethodVisitor = mv
  }

  def process(className: String): Unit = {
    backlog += className
    processRecursively()
  }

  private def enqueue(className: String): Unit = {
    val exclude = exclusions.exists(exc => className.startsWith(exc))
    if (className.startsWith("[L") && className.endsWith(";")) {
      enqueue(className.substring(2, className.length - 1))
    } else if (className.indexOf('/') > -1 && !exclude && !referenced.contains(className)) {
      backlog += className
      referenced += className
    } else {
      logger.debug(s"Excluding: $className")
    }
  }

  @tailrec
  private def processRecursively(): Unit = {
    val className = backlog.dequeue()
    logger.info(s"Processing $className (referenced: ${referenced.size}, backlog: ${backlog.size})...")
    referenced += className

    val entry = jar.getEntry(s"$className.class")
    val input = jar.getInputStream(entry)
    try {
      val classReader = new ClassReader(input)
      classReader.accept(cv, 0)
    } finally {
      input.close()
    }

    if (backlog.nonEmpty) {
      processRecursively()
    }
  }
}