package com.outr.jefe.launch.jmx

import org.powerscala.StringUtil

case class MemoryUsage(committed: Long, init: Long, max: Long, used: Long) {
  override def toString: String = f"{ committed=${StringUtil.humanReadableByteCount(committed)}, init=${StringUtil.humanReadableByteCount(init)}, max=${StringUtil.humanReadableByteCount(max)}, used=${StringUtil.humanReadableByteCount(used)} }"  }
