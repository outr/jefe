package com.outr.jefe.server

import java.io.File

case class LocalCommand(value: String, configuration: MainConfiguration, baseDirectory: File) {
  def toRemote: RemoteCommand = RemoteCommand(value, configuration.password, baseDirectory.getCanonicalPath)
}
