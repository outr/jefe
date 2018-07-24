package com.outr.jefe.server.service

import com.outr.jefe.application.ApplicationConfig
import com.outr.jefe.server.JefeServer
import com.outr.jefe.model.BasicResponse
import io.youi.ValidationError
import io.youi.http.{HttpConnection, HttpStatus}
import io.youi.server.rest.{Restful, RestfulResponse}

import scala.concurrent.Future

object CreateApplication extends Restful[ApplicationConfig, BasicResponse] {
  override def apply(connection: HttpConnection, request: ApplicationConfig): Future[RestfulResponse[BasicResponse]] = {
    val application = request.create()
    JefeServer.applications += application
    Future.successful(RestfulResponse(BasicResponse(success = true, errors = Nil), HttpStatus.OK))
  }

  override def error(errors: List[ValidationError], status: HttpStatus): RestfulResponse[BasicResponse] = {
    RestfulResponse(BasicResponse(success = false, errors = errors), status)
  }
}