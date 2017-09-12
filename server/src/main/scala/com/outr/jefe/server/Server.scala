package com.outr.jefe.server

import io.youi.Priority
import io.youi.http._
import io.circe.generic.auto._

object Server extends io.youi.server.Server {
  handler.matcher(path.exact("/jefe/remote")).priority(Priority.High).restful[RemoteCommand, RemoteResponse] { command =>
    val success = Jefe.run(Jefe.localize(command))
    RemoteResponse(Nil, success)
  }
}