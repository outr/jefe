package com.outr.jefe.server.service

import com.outr.jefe.launch.jmx.JMXProcessMonitor
import com.outr.jefe.model.ListResponse
import com.outr.jefe.server.JefeServer
import io.youi.ValidationError
import io.youi.http.{HttpConnection, HttpStatus}
import io.youi.server.rest.{Restful, RestfulResponse}

import scala.concurrent.Future

object ListApplications extends Restful[Unit, ListResponse] {
  override def apply(connection: HttpConnection, request: Unit): Future[RestfulResponse[ListResponse]] = {
    val applications = JefeServer.applications.all().map { app =>
      app.stats()
    }
    Future.successful(RestfulResponse(ListResponse(
      stats = JMXProcessMonitor.stats(),
      applicationStats = applications,
      success = true,
      errors = Nil
    ), HttpStatus.OK))
  }

  override def error(errors: List[ValidationError], status: HttpStatus): RestfulResponse[ListResponse] = {
    RestfulResponse(ListResponse(JMXProcessMonitor.stats(), Nil, success = false, errors = errors), status)
  }
}