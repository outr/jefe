package com.outr.jefe.launch

trait Launcher {
  def launch(): Launched
}

// TODO: WARLauncher extends JARLauncher
// TODO: StaticSiteLauncher extends JARLauncher