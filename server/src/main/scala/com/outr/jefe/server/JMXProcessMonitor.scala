package com.outr.jefe.server

import java.lang.management.{ClassLoadingMXBean, ManagementFactory, MemoryMXBean, RuntimeMXBean, ThreadMXBean}
import javax.management.MBeanServerConnection
import javax.management.remote.{JMXConnectorFactory, JMXServiceURL}

import com.sun.management.OperatingSystemMXBean
import org.powerscala.StringUtil
import org.powerscala.concurrent.Time

class JMXProcessMonitor(port: Int) {
  def stats(): ProcessStats = {
    val url = new JMXServiceURL(s"service:jmx:rmi:///jndi/rmi://localhost:$port/jmxrmi")
    val jmxc = JMXConnectorFactory.connect(url, null)
    try {
      implicit val connection = jmxc.getMBeanServerConnection

      val mxRT = instance[RuntimeMXBean](ManagementFactory.RUNTIME_MXBEAN_NAME)
      val startTime = mxRT.getStartTime
      val upTime = mxRT.getUptime

      val mxOS = instance[OperatingSystemMXBean](ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME)
      val loadAverage = mxOS.getSystemLoadAverage
      val load = mxOS.getProcessCpuLoad
      val time = mxOS.getProcessCpuTime
      val systemLoad = mxOS.getSystemCpuLoad
      val os = OperatingSystem(startTime, upTime, loadAverage, load, time, systemLoad)

      val mxMem = instance[MemoryMXBean](ManagementFactory.MEMORY_MXBEAN_NAME)
      val onHeap = mxMem.getHeapMemoryUsage
      val offHeap = mxMem.getNonHeapMemoryUsage
      val heapUsage = MemoryUsage(onHeap.getCommitted, onHeap.getInit, onHeap.getMax, onHeap.getUsed)
      val nonHeapUsage = MemoryUsage(offHeap.getCommitted, offHeap.getInit, offHeap.getMax, offHeap.getUsed)

      val mxCL = instance[ClassLoadingMXBean](ManagementFactory.CLASS_LOADING_MXBEAN_NAME)
      val classLoading = ClassLoading(mxCL.getLoadedClassCount, mxCL.getTotalLoadedClassCount, mxCL.getUnloadedClassCount)

      val mxT = instance[ThreadMXBean](ManagementFactory.THREAD_MXBEAN_NAME)
      val threading = Threading(mxT.getDaemonThreadCount, mxT.getPeakThreadCount, mxT.getThreadCount, mxT.getTotalStartedThreadCount)

      ProcessStats(os, heapUsage, nonHeapUsage, threading, classLoading)
    } finally {
      jmxc.close()
    }
  }

  private def instance[T](objectName: String)(implicit manifest: Manifest[T], connection: MBeanServerConnection): T = {
    ManagementFactory.newPlatformMXBeanProxy[T](connection, objectName, manifest.runtimeClass.asInstanceOf[Class[T]])
  }

  private def durationFromMillisToHumanReadable(duration: Long): String = Time.elapsed(duration).shorthand

  case class ProcessStats(os: OperatingSystem,
                          heapUsage: MemoryUsage,
                          nonHeapUsage: MemoryUsage,
                          threading: Threading,
                          classLoading: ClassLoading) {
    def toList: List[String] = List(
      s"OS: $os",
      s"Heap Usage: $heapUsage",
      s"Non Heap Usage: $nonHeapUsage",
      s"Threading: $threading",
      s"Class Loading: $classLoading"
    )

    override def toString: String = toList.mkString("\n")
  }

  case class OperatingSystem(startTime: Long, upTime: Long, loadAverage: Double, load: Double, time: Long, systemLoad: Double) {
    override def toString: String = f"{ startTime=$startTime%tF, upTime=${durationFromMillisToHumanReadable(upTime)}, loadAverage=$loadAverage%2.2f, load=$load%2.2f, time=${durationFromMillisToHumanReadable(time)}, systemLoad=$systemLoad%2.2f }"
  }

  case class MemoryUsage(committed: Long, init: Long, max: Long, used: Long) {
    override def toString: String = f"{ committed=${StringUtil.humanReadableByteCount(committed)}, init=${StringUtil.humanReadableByteCount(init)}, max=${StringUtil.humanReadableByteCount(max)}, used=${StringUtil.humanReadableByteCount(used)} }"  }

  case class ClassLoading(loaded: Int, total: Long, unloaded: Long) {
    override def toString: String = f"{ loaded=$loaded, unloaded=$unloaded, total=$total }"
  }

  case class Threading(daemon: Int, peak: Int, count: Int, total: Long) {
    override def toString: String = f"{ daemon=$daemon, peak=$peak, total=$total }"
  }
}