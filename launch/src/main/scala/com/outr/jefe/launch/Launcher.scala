package com.outr.jefe.launch

import scribe.{Level, LogRecord, Logger, MDC}
import scribe.format._
import scribe.output.{CompositeOutput, LogOutput}

trait Launcher {
  def name: String
  def loggerId: Long
  def logger: Logger = Logger(loggerId)
  def launch(): Launched
}

object Launcher {
  private val formatter = new Formatter {
    override def format[M](record: LogRecord[M]): LogOutput = MDC.get("application") match {
      case Some(application) => new CompositeOutput(List(
        string("["),
        cyan(string(application)),
        string("] "),
        message,
        newLine
      ).map(_.format(record)))
      case None => Formatter.simple.format(record)
    }
  }

  val loggerId: Long = Logger
    .empty
    .orphan()
    .withHandler(formatter = formatter, minimumLevel = Some(Level.Info))
    .replace()
    .id

  def logger: Logger = Logger(loggerId)
}