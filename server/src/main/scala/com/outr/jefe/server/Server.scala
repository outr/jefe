package com.outr.jefe.server

import io.youi.Priority
import io.youi.http._
import io.circe.generic.auto._

object Server extends io.youi.server.Server {
  handler.matcher(path.exact("/jefe/remote")).priority(Priority.High).restful[RemoteCommand, RemoteResponse] { command =>
    Jefe.run(command)
  }
  handler.matcher(all).handle { connection =>
    if (connection.response.content.isEmpty) {
      Jefe.access.warn(s"No content returned for ${connection.request.url}")
    }
  }
}