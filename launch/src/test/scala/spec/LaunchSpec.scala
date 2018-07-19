package spec

import com.outr.jefe.launch.{ProcessLauncher, ProcessStoppedStatus}
import org.scalatest.{Matchers, WordSpec}
import scribe.{LogRecord, Logger}
import scribe.writer.Writer

import scala.collection.mutable.ListBuffer

class LaunchSpec extends WordSpec with Matchers with Writer {
  private var records = ListBuffer.empty[LogRecord[_]]

  "Launcher" should {
    "launch a simple script" in {
      val logger = Logger.empty.orphan().withHandler(writer = this).replace()
      val launcher = new ProcessLauncher(List("./test1.sh"), loggerId = logger.id)
      val launched = launcher.launch()
      val status = launched.waitForFinished()
      status should be(ProcessStoppedStatus(0))
      records.toList.map(_.message) should be(List("Test Script 1", "Delayed...", "Finishing!"))
    }
  }

  override def write[M](record: LogRecord[M], output: String): Unit = synchronized {
    records += record
  }
}
