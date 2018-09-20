package com.outr.jefe.launch

import java.io.File
import java.nio.file.attribute.{PosixFilePermission, PosixFilePermissions}
import java.nio.file.{Files, Path}

import com.outr.jefe.Jefe
import scribe.Logger
import scala.collection.JavaConverters._

class ProcessLauncher(val commands: List[String],
                      val workingDirectory: File = new File("."),
                      val environment: Map[String, String] = Map.empty,
                      val loggerId: Long = Logger.root.id,
                      val background: Boolean = false) extends Launcher {
  private lazy val script = Files.createTempFile(Jefe.baseDirectory, "script", ".sh")

  private val commandsList = if (background) {
    ProcessLauncher.createScript(script, commands.mkString(" "))
    List(ProcessLauncher.backgroundScript.toString, script.toString)
  } else {
    commands
  }

  private lazy val processBuilder = {
    val b = new ProcessBuilder(commandsList: _*)
    b.directory(workingDirectory)
    val env = b.environment()
    environment.foreach {
      case (key, value) => env.put(key, value)
    }
    b
  }

  override def launch(): Launched = try {
    scribe.info(s"Launching (background: $background): ${commandsList.mkString(" ")}")
    val process = processBuilder.start()
    if (background) {
      Runtime.getRuntime.addShutdownHook(new Thread {
        override def run(): Unit = {
          Files.deleteIfExists(script)
        }
      })
    }
    LaunchedProcess(this, process)
  } catch {
    case t: Throwable => FailedProcess(this, t)
  }
}

object ProcessLauncher {
  def createScript(file: Path, content: String): Path = {
    scribe.info(s"Creating script: $content")
    Files.deleteIfExists(file)
    Files.createFile(file)
    val bashScript =
      s"""#!/bin/bash
         |
         |$content
       """.stripMargin
    Files.write(file, bashScript.getBytes("UTF-8"))
    val permissions = Set(
      PosixFilePermission.OWNER_READ,
      PosixFilePermission.OWNER_WRITE,
      PosixFilePermission.OWNER_EXECUTE
    )
    Files.setPosixFilePermissions(file, permissions.asJava)
    file
  }

  lazy val backgroundScript: Path = {
    val script = Jefe.baseDirectory.resolve("background.sh")
    createScript(script, """nohup "$@" &""".stripMargin)
  }
}