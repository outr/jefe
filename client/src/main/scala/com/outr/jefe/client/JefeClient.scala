package com.outr.jefe.client

import com.outr.jefe.application.Application
import com.outr.jefe.model.{ApplicationActionRequest, BasicResponse, StatsResponse}
import com.outr.jefe.server.ProxyConfig
import io.youi.client.HttpClient
import io.youi.net._

import scala.concurrent.Future
import scribe.Execution.global

class JefeClient(baseURL: URL, token: String) {
  private lazy val client = HttpClient.header("jefe.token", token)

  object application {
    def create(application: Application): Future[BasicResponse] = {
      client
        .path(path"/application/create")
        .restful[Application, BasicResponse](application)
    }

    def start(applicationId: String): Future[BasicResponse] = {
      client
        .path(path"/application/start")
        .restful[ApplicationActionRequest, BasicResponse](ApplicationActionRequest(applicationId))
    }

    def stats(applicationId: String): Future[StatsResponse] = {
      client
        .path(path"/application/stats")
        .restful[ApplicationActionRequest, StatsResponse](ApplicationActionRequest(applicationId))
    }

    def stop(applicationId: String): Future[BasicResponse] = {
      client
        .path(path"/application/stop")
        .restful[ApplicationActionRequest, BasicResponse](ApplicationActionRequest(applicationId))
    }

    def reStart(applicationId: String): Future[BasicResponse] = {
      client
        .path(path"/application/restart")
        .restful[ApplicationActionRequest, BasicResponse](ApplicationActionRequest(applicationId))
    }

    def remove(applicationId: String): Future[BasicResponse] = {
      client
        .path(path"/application/remove")
        .restful[ApplicationActionRequest, BasicResponse](ApplicationActionRequest(applicationId))
    }
  }

  object proxy {
    def add(config: ProxyConfig): Future[BasicResponse] = {
      client
        .path(path"/proxy/add")
        .restful[ProxyConfig, BasicResponse](config)
    }
    def remove(config: ProxyConfig): Future[BasicResponse] = {
      client
        .path(path"/proxy/remove")
        .restful[ProxyConfig, BasicResponse](config)
    }
  }

  def stop(): Future[BasicResponse] = {
    client
      .path(path"/stop")
      .restful[Unit, BasicResponse](())
  }
}
