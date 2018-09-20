package com.outr.jefe.server

import com.outr.jefe.model.BasicResponse
import io.youi.ValidationError
import io.youi.http.{Content, HeaderKey, HttpConnection, HttpStatus}
import io.youi.net.ContentType
import io.youi.server.dsl.ConnectionFilter
import profig.JsonUtil

object SecurityFilter extends ConnectionFilter {
  val FailureCode: Int = 9

  override def filter(connection: HttpConnection): Option[HttpConnection] = {
    val token = connection.request.headers.first(HeaderKey("jefe.token")).getOrElse("")
    if (JefeServer.token != token) {
      val ve = ValidationError(
        message = if (token.isEmpty) {
          "Invalid request, no jefe.token specified in the header"
        } else {
          "Invalid request, bad jefe.token specified"
        },
        code = FailureCode,
        status = HttpStatus.Forbidden
      )
      val json = JsonUtil.toJsonString(BasicResponse(success = false, errors = List(ve)))
      connection.update { response =>
        response
          .withContent(Content.string(json, ContentType.`application/json`))
          .withStatus(HttpStatus.Forbidden)
      }
      None
    } else {
      Some(connection)
    }
  }
}