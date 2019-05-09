package com.outr.jefe.server

import com.outr.jefe.model.BasicResponse
import io.youi.ValidationError
import io.youi.http.content.Content
import io.youi.http.{HeaderKey, HttpConnection, HttpStatus}
import io.youi.server.dsl.{ConnectionFilter, FilterResponse}
import profig.JsonUtil

import scala.concurrent.Future

object SecurityFilter extends ConnectionFilter {
  val FailureCode: Int = 9

  override def filter(connection: HttpConnection): Future[FilterResponse] = {
    val token = connection.request.headers.first(HeaderKey("jefe.token")).getOrElse("")
    if (connection.request.url.path.toString().startsWith("/jefe") && JefeServer.token != token) {
      scribe.warn(s"Security failure, invalid token specified: $token (actual token: ${JefeServer.token})")
      val ve = ValidationError(
        message = if (token.isEmpty) {
          "Invalid request, no jefe.token specified in the header"
        } else {
          s"Invalid request, bad jefe.token specified ($token)"
        },
        code = FailureCode,
        status = HttpStatus.Forbidden
      )
      val json = JsonUtil.toJson(BasicResponse(success = false, errors = List(ve)))
      Future.successful(stop(connection.modify { response =>
        response.withContent(Content.json(json)).withStatus(HttpStatus.Forbidden)
      }))
    } else {
      Future.successful(continue(connection))
    }
  }
}