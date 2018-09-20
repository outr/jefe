package com.outr.jefe.server.service

import com.outr.jefe.model.BasicResponse
import com.outr.jefe.server.JefeServer
import io.youi.ValidationError
import io.youi.http.{HttpConnection, HttpStatus}
import io.youi.server.rest.{Restful, RestfulResponse}

import scala.concurrent.Future
import scribe.Execution.global

object StopServer extends Restful[Unit, BasicResponse] {
  override def apply(connection: HttpConnection, request: Unit): Future[RestfulResponse[BasicResponse]] = {
    scribe.warn("Server requested to stop. Shutting down in ten seconds...")
    Future {
      Thread.sleep(10000L)
      JefeServer.dispose()
    }

    Future.successful(RestfulResponse(BasicResponse(success = true, errors = Nil), HttpStatus.OK))
  }

  override def error(errors: List[ValidationError], status: HttpStatus): RestfulResponse[BasicResponse] = {
    RestfulResponse(BasicResponse(success = false, errors = errors), status)
  }
}
