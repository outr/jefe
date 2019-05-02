package com.outr.jefe

import java.io.File
import java.nio.file.Path

object Jefe {
  var baseDirectory: Path = new File(".").toPath
}