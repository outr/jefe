package com.outr.jefe.server

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Http, Service}
import com.twitter.util.{Await, Future}

object TestProxy extends App {
  val client = Http.newService("www.hyperscala.org:80")
  val proxy = new Service[Request, Response] {
    override def apply(request: Request): Future[Response] = {
      request.headerMap.set("Host", "www.hyperscala.org")
      client(request)
    }
  }

  val server = Http.serve(":8080", proxy)
  Await.ready(server)
}