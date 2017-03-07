package com.outr.jefe

import java.io.File

import io.youi.app.SinglePageApplication
import io.youi.http._
import io.youi.server.UndertowServer

class JefeConsoleServer(override val templateDirectory: File) extends UndertowServer with SinglePageApplication with JefeApplication {
  override protected def appJSContent: Content = Content.classPath("app/jefe-console-fastopt.js")
  override protected def appJSMethod: String = "application"

  handler.matcher(combined.any(path.exact("/"), path.exact("/index.html"))).htmlPage()
}

object JefeConsoleServer {
  def main(args: Array[String]): Unit = {
    val server = new JefeConsoleServer(new File("."))
    server.start()
  }
}