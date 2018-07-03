package com.outr.jefe.server.service

import com.outr.jefe.server.JefeServer
import com.outr.jefe.server.service.model.{ApplicationActionRequest, StatsResponse}
import io.youi.ValidationError
import io.youi.http.{HttpConnection, HttpStatus}
import io.youi.server.rest.{Restful, RestfulResponse}

import scala.concurrent.Future

object StatsApplication extends Restful[ApplicationActionRequest, StatsResponse] {
  override def apply(connection: HttpConnection, request: ApplicationActionRequest): Future[RestfulResponse[StatsResponse]] = {
    val response = JefeServer.applications.byId(request.applicationId) match {
      case Some(application) => {
        val stats = application.stats()
        RestfulResponse(StatsResponse(stats = stats, errors = Nil), HttpStatus.OK)
      }
      case None => {
        RestfulResponse(StatsResponse(stats = None, errors = List(ValidationError(s"Application not found by id: ${request.applicationId}"))), HttpStatus.ExpectationFailed)
      }
    }
    Future.successful(response)
  }

  override def error(errors: List[ValidationError], status: HttpStatus): RestfulResponse[StatsResponse] = {
    RestfulResponse(StatsResponse(stats = None, errors = errors), status)
  }
}