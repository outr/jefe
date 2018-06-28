package com.outr.jefe.launch

trait Launcher {
  def launch(): Launched
}

object Launcher {
  private lazy val javaHome = System.getProperty("java.home")
  private lazy val fileSeparator = System.getProperty("file.separator")
  private lazy val pathSeparator = System.getProperty("path.separator")

  lazy val Java: String = {
    val extension = if (fileSeparator == "/") "" else "w.exe"
    s"$javaHome${fileSeparator}bin${fileSeparator}java$extension"
  }
}

// TODO: JARLauncher extends ProcessLauncher
// TODO: WARLauncher extends JARLauncher
// TODO: StaticSiteLauncher extends JARLauncher