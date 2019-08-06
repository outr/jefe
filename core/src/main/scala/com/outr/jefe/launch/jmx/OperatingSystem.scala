package com.outr.jefe.launch.jmx

case class OperatingSystem(startTime: Long, upTime: Long, loadAverage: Double, load: Double, time: Long, systemLoad: Double) {
  private val Second = 1.0
  private val Minute = 60.0 * Second
  private val Hour = 60.0 * Minute
  private val Day = 24.0 * Hour
  private val Week = 7.0 * Day
  private val Month = 30.0 * Day
  private val Year = 365.0 * Day

  private def durationFromMillisToHumanReadable(duration: Long): String = shorthand(duration)

  private def shorthand(duration: Double) = {
    var value = duration
    val ending = if (time > Year) {
      value = time / Year
      "year"
    } else if (time > Month) {
      value = time / Month
      "month"
    } else if (time > Week) {
      value = time / Week
      "week"
    } else if (time > Day) {
      value = time / Day
      "day"
    } else if (time > Hour) {
      value = time / Hour
      "hour"
    } else if (time > Minute) {
      value = time / Minute
      "minute"
    } else if (time > Second) {
      value = time / Second
      "second"
    } else {
      "ms"
    }
    val round = math.round(value)
    val s = if (round != 1 && ending != "ms") "s" else ""

    s"$round $ending$s"
  }

  override def toString: String = f"{ startTime=$startTime%tF, upTime=${durationFromMillisToHumanReadable(upTime)}, loadAverage=$loadAverage%2.2f, load=$load%2.2f, time=${durationFromMillisToHumanReadable(time)}, systemLoad=$systemLoad%2.2f }"
}
