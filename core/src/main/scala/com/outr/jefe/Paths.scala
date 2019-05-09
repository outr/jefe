package com.outr.jefe

import io.youi.net._

object Paths {
  object application {
    val create: Path = path"/jefe/application/create"
    val start: Path = path"/jefe/application/start"
    val stats: Path = path"/jefe/application/stats"
    val list: Path = path"/jefe/application/list"
    val stop: Path = path"/jefe/application/stop"
    val save: Path = path"/jefe/application/save"
    val restart: Path = path"/jefe/application/restart"
    val remove: Path = path"/jefe/application/remove"
  }
  object proxy {
    val add: Path = path"/jefe/proxy/add"
    val remove: Path = path"/jefe/proxy/remove"
  }
  val version: Path = path"/jefe/version"
  val stop: Path = path"/jefe/stop"
}