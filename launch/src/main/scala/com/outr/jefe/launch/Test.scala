package com.outr.jefe.launch

import java.io.File

object Test {
  // TODO: Migrate to test
  def main(args: Array[String]): Unit = {
    scribe.info(s"Base Directory: ${new File(".").getCanonicalPath}")
    val launcher = new ProcessLauncher(List("./test1.sh"))
    scribe.info("Created launcher!")
    val launched = launcher.launch()
    scribe.info("Launched! Waiting for finished...")
    val finished = launched.waitForFinished()
    scribe.info(s"Finished: $finished")
  }
}
