package com.outr.jefe.launch.jmx

import org.powerscala.concurrent.Time

case class OperatingSystem(startTime: Long, upTime: Long, loadAverage: Double, load: Double, time: Long, systemLoad: Double) {
  private def durationFromMillisToHumanReadable(duration: Long): String = Time.elapsed(duration).shorthand

  override def toString: String = f"{ startTime=$startTime%tF, upTime=${durationFromMillisToHumanReadable(upTime)}, loadAverage=$loadAverage%2.2f, load=$load%2.2f, time=${durationFromMillisToHumanReadable(time)}, systemLoad=$systemLoad%2.2f }"
}
