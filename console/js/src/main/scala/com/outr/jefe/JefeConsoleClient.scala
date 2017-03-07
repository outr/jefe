package com.outr.jefe

import io.youi.UI
import io.youi.app.ClientApplication

import scala.scalajs.js.annotation.JSExportTopLevel

object JefeConsoleClient extends JefeApplication with ClientApplication {


  @JSExportTopLevel("application")
  def main(): Unit = {
    scribe.info("Client loaded!")

    UI.init()
    UI.title := "JEFE Console"
  }
}
