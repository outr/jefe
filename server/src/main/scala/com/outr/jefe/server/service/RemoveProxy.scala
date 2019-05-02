package com.outr.jefe.server.service

import com.outr.jefe.model.BasicResponse
import com.outr.jefe.server.{JefeServer, ProxyConfig}
import io.youi.ValidationError
import io.youi.http.{HttpConnection, HttpStatus}
import io.youi.server.rest.{Restful, RestfulResponse}

import scala.concurrent.Future

object RemoveProxy extends Restful[ProxyConfig, BasicResponse] {
  override def apply(connection: HttpConnection, request: ProxyConfig): Future[RestfulResponse[BasicResponse]] = {
    JefeServer.proxies -= request
    JefeServer.save()
    Future.successful(RestfulResponse(BasicResponse(success = true, errors = Nil), HttpStatus.OK))
  }

  override def error(errors: List[ValidationError], status: HttpStatus): RestfulResponse[BasicResponse] = {
    RestfulResponse(BasicResponse(success = false, errors = errors), status)
  }
}