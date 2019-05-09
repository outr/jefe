package com.outr.jefe.client

import com.outr.jefe.Paths
import com.outr.jefe.application.Application
import com.outr.jefe.model.{ApplicationActionRequest, BasicResponse, ListResponse, StatsResponse}
import com.outr.jefe.server.ProxyConfig
import io.youi.client.HttpClient
import io.youi.client.intercept.Interceptor
import io.youi.http.{HttpRequest, HttpResponse, HttpStatus}
import io.youi.net.URL

import scala.concurrent.Future
import scribe.Execution.global

class JefeClient(baseURL: URL, token: String) extends Interceptor {
  private lazy val client = HttpClient
    .header("jefe.token", token)
    .url(baseURL)
    .noFailOnHttpStatus
    .interceptor(this)

  object application {
    def create(application: Application): Future[BasicResponse] = {
      client
        .path(Paths.application.create)
        .restful[Application, BasicResponse](application)
    }

    def start(applicationId: String): Future[BasicResponse] = {
      client
        .path(Paths.application.start)
        .restful[ApplicationActionRequest, BasicResponse](ApplicationActionRequest(applicationId))
    }

    def stats(applicationId: String): Future[StatsResponse] = {
      client
        .path(Paths.application.stats)
        .restful[ApplicationActionRequest, StatsResponse](ApplicationActionRequest(applicationId))
    }

    def list(): Future[ListResponse] = {
      client
        .path(Paths.application.list)
        .restful[Unit, ListResponse](())
    }

    def stop(applicationId: String): Future[BasicResponse] = {
      client
        .path(Paths.application.stop)
        .restful[ApplicationActionRequest, BasicResponse](ApplicationActionRequest(applicationId))
    }

    def save(): Future[BasicResponse] = {
      client
        .path(Paths.application.save)
        .restful[Unit, BasicResponse](())
    }

    def reStart(applicationId: String): Future[BasicResponse] = {
      client
        .path(Paths.application.restart)
        .restful[ApplicationActionRequest, BasicResponse](ApplicationActionRequest(applicationId))
    }

    def remove(applicationId: String): Future[BasicResponse] = {
      client
        .path(Paths.application.remove)
        .restful[ApplicationActionRequest, BasicResponse](ApplicationActionRequest(applicationId))
    }
  }

  object proxy {
    def add(config: ProxyConfig): Future[BasicResponse] = {
      client
        .path(Paths.proxy.add)
        .restful[ProxyConfig, BasicResponse](config)
    }
    def remove(config: ProxyConfig): Future[BasicResponse] = {
      client
        .path(Paths.proxy.remove)
        .restful[ProxyConfig, BasicResponse](config)
    }
  }

  def stop(): Future[BasicResponse] = {
    client
      .path(Paths.stop)
      .restful[Unit, BasicResponse](())
  }

  override def before(request: HttpRequest): Future[HttpRequest] = Future.successful(request)

  override def after(request: HttpRequest, response: HttpResponse): Future[HttpResponse] = {
    if (response.status != HttpStatus.OK) {
      scribe.warn(s"[${request.url}] ${request.method}: ${request.content.map(_.asString).getOrElse("")}")
      scribe.warn(s"[${request.url.decoded}] ${response.status.message} (${response.status.code}) Received: ${response.content.map(_.asString).getOrElse("")}")
    }
    Future.successful(response)
  }
}
