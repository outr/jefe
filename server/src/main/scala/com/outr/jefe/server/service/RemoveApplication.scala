package com.outr.jefe.server.service

import com.outr.jefe.server.JefeServer
import com.outr.jefe.model.{ApplicationActionRequest, BasicResponse}
import io.youi.ValidationError
import io.youi.http.{HttpConnection, HttpStatus}
import io.youi.server.rest.{Restful, RestfulResponse}

import scala.concurrent.Future

object RemoveApplication extends Restful[ApplicationActionRequest, BasicResponse] {
  override def apply(connection: HttpConnection, request: ApplicationActionRequest): Future[RestfulResponse[BasicResponse]] = {
    val response = JefeServer.applications.byId(request.applicationId) match {
      case Some(application) => {
        JefeServer.applications -= application
        RestfulResponse(BasicResponse(success = true, errors = Nil), HttpStatus.OK)
      }
      case None => {
        RestfulResponse(BasicResponse(success = false, errors = List(ValidationError(s"Application not found by id: ${request.applicationId}"))), HttpStatus.ExpectationFailed)
      }
    }
    Future.successful(response)
  }

  override def error(errors: List[ValidationError], status: HttpStatus): RestfulResponse[BasicResponse] = {
    RestfulResponse(BasicResponse(success = false, errors = errors), status)
  }
}
