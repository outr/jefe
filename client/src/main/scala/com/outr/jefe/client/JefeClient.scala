package com.outr.jefe.client

import com.outr.jefe.application.ApplicationConfig
import com.outr.jefe.model.{ApplicationActionRequest, BasicResponse, StatsResponse}
import io.youi.client.HttpClient
import io.youi.http.Headers
import io.youi.net._
import io.circe.generic.auto._

import scala.concurrent.Future

class JefeClient(baseURL: URL, token: String) {
  private lazy val client = HttpClient()
  private lazy val headers = Headers.empty.withHeader("jefe.token", token)

  object application {
    def create(config: ApplicationConfig): Future[BasicResponse] = {
      val url = baseURL.copy(path = path"/application/create")
      client.restful[ApplicationConfig, BasicResponse](url, config, headers)
    }

    def start(applicationId: String): Future[BasicResponse] = {
      val url = baseURL.copy(path = path"/application/start")
      client.restful[ApplicationActionRequest, BasicResponse](url, ApplicationActionRequest(applicationId), headers)
    }

    def stats(applicationId: String): Future[StatsResponse] = {
      val url = baseURL.copy(path = path"/application/stats")
      client.restful[ApplicationActionRequest, StatsResponse](url, ApplicationActionRequest(applicationId), headers)
    }

    def stop(applicationId: String): Future[BasicResponse] = {
      val url = baseURL.copy(path = path"/application/stop")
      client.restful[ApplicationActionRequest, BasicResponse](url, ApplicationActionRequest(applicationId), headers)
    }

    def reStart(applicationId: String): Future[BasicResponse] = {
      val url = baseURL.copy(path = path"/application/restart")
      client.restful[ApplicationActionRequest, BasicResponse](url, ApplicationActionRequest(applicationId), headers)
    }

    def remove(applicationId: String): Future[BasicResponse] = {
      val url = baseURL.copy(path = path"/application/remove")
      client.restful[ApplicationActionRequest, BasicResponse](url, ApplicationActionRequest(applicationId), headers)
    }
  }
}