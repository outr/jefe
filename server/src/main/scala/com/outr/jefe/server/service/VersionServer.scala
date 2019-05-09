package com.outr.jefe.server.service

import com.outr.jefe.BuildInfo
import io.youi.ValidationError
import io.youi.http.{HttpConnection, HttpStatus}
import io.youi.server.rest.{Restful, RestfulResponse}

import scala.concurrent.Future

object VersionServer extends Restful[Unit, String] {
  override def apply(connection: HttpConnection, request: Unit): Future[RestfulResponse[String]] = {
    Future.successful(RestfulResponse(BuildInfo.version, HttpStatus.OK))
  }

  override def error(errors: List[ValidationError], status: HttpStatus): RestfulResponse[String] = {
    RestfulResponse(s"Error: ${errors.mkString(", ")}", status)
  }
}