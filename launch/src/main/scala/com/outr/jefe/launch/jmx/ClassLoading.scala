package com.outr.jefe.launch.jmx

case class ClassLoading(loaded: Int, total: Long, unloaded: Long) {
  override def toString: String = f"{ loaded=$loaded, unloaded=$unloaded, total=$total }"
}
