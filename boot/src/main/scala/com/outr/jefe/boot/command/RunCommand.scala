package com.outr.jefe.boot.command

import com.outr.jefe.application._

object RunCommand extends ApplicationCommand {
  override def name: String = "run"
  override def description: String = "Runs an ad-hoc process"

  override def execute(application: Application): Unit = {
    application.start()
    application match {
      case pa: ProcessApplication if !pa.background => pa.waitForFinished()
      case _ => // Not a ProcessApplication
    }
  }

  override def helpArguments: List[(String, String)] = super.helpArguments ::: List(
    "background" -> "If true, starts as a background process that will continue running when the terminal exits (nohup). Defaults to false."
  )
}