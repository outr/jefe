package com.outr.jefe.launch

class ProcessLauncherInstance(builder: ProcessBuilder) extends LauncherInstance {
  private lazy val process = builder.inheritIO().start()

  override def start(): Unit = {
    _status := LauncherStatus.Starting
    process
    new Thread {
      // TODO: handle this better
      override def run(): Unit = {
        _status := LauncherStatus.Running
        while (process.isAlive) {
          Thread.sleep(10)
        }
        val exitValue = process.exitValue()
        if (exitValue == 0) {

        }
        _status := LauncherStatus.Finished
      }
    }
  }

  override def stop(): Unit = {
    process.destroy()
  }
}
