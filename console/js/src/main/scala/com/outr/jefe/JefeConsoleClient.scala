package com.outr.jefe

import io.youi._
import io.youi.app.ClientApplication
import reactify.Var

import scala.scalajs.js.annotation.JSExportTopLevel

object JefeConsoleClient extends JefeApplication with ClientApplication {
  val colorScheme: Var[ColorScheme] = Var(ColorScheme.Solarized.Dark)

  @JSExportTopLevel("application")
  def main(): Unit = {
    scribe.info("Client loaded!")

    ui.init()
    ui.title := "JEFE Console"
    ui.backgroundColor := colorScheme().base3
  }
}
