package com.outr.appmanager.launch

class ClassLoaderLauncherInstance(classLoader: ClassLoader, runner: () => Unit) extends LauncherInstance {
  val runnable = new Runnable {
    override def run(): Unit = {
      _status := LauncherStatus.Running
      try {
        runner()
      } catch {
        case t: Throwable => _error := Some(t)
      } finally {
        _status := LauncherStatus.Finished
      }
    }
  }
  val thread = new Thread(runnable)

  override def start(): Unit = {
    _status := LauncherStatus.Starting
    thread.start()
  }
}
