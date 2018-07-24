package com.outr.jefe.launch.jmx

case class Threading(daemon: Int, peak: Int, count: Int, total: Long) {
  override def toString: String = f"{ daemon=$daemon, peak=$peak, total=$total }"
}
