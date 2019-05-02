package com.outr.jefe.launch.jmx

import java.lang.management._

import com.sun.management.OperatingSystemMXBean
import javax.management.MBeanServerConnection
import javax.management.remote.{JMXConnectorFactory, JMXServiceURL}

object JMXProcessMonitor {
  def stats(config: JMXConfig): ProcessStats = {
    val url = new JMXServiceURL(s"service:jmx:rmi:///jndi/rmi://${config.host}:${config.port}/jmxrmi")
    val jmxConnection = JMXConnectorFactory.connect(url, null)
    try {
      implicit val mBeanServerConnection: MBeanServerConnection = jmxConnection.getMBeanServerConnection

      val mxRT = instance[RuntimeMXBean](ManagementFactory.RUNTIME_MXBEAN_NAME)
      val startTime = mxRT.getStartTime
      val upTime = mxRT.getUptime

      val id = mxRT.getName

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

      ProcessStats(id, os, heapUsage, nonHeapUsage, threading, classLoading)
    } finally {
      jmxConnection.close()
    }
  }

  def stats(): ProcessStats = {
    val mxRT = ManagementFactory.getRuntimeMXBean
    val startTime = mxRT.getStartTime
    val upTime = mxRT.getUptime

    val id = mxRT.getName

    val mxOS = ManagementFactory.getOperatingSystemMXBean.asInstanceOf[OperatingSystemMXBean]
    val loadAverage = mxOS.getSystemLoadAverage
    val load = mxOS.getProcessCpuLoad
    val time = mxOS.getProcessCpuTime
    val systemLoad = mxOS.getSystemCpuLoad
    val os = OperatingSystem(startTime, upTime, loadAverage, load, time, systemLoad)

    val mxMem = ManagementFactory.getMemoryMXBean
    val onHeap = mxMem.getHeapMemoryUsage
    val offHeap = mxMem.getNonHeapMemoryUsage
    val heapUsage = MemoryUsage(onHeap.getCommitted, onHeap.getInit, onHeap.getMax, onHeap.getUsed)
    val nonHeapUsage = MemoryUsage(offHeap.getCommitted, offHeap.getInit, offHeap.getMax, offHeap.getUsed)

    val mxCL = ManagementFactory.getClassLoadingMXBean
    val classLoading = ClassLoading(mxCL.getLoadedClassCount, mxCL.getTotalLoadedClassCount, mxCL.getUnloadedClassCount)

    val mxT = ManagementFactory.getThreadMXBean
    val threading = Threading(mxT.getDaemonThreadCount, mxT.getPeakThreadCount, mxT.getThreadCount, mxT.getTotalStartedThreadCount)

    ProcessStats(id, os, heapUsage, nonHeapUsage, threading, classLoading)
  }

  private def instance[T](objectName: String)(implicit manifest: Manifest[T], connection: MBeanServerConnection): T = {
    ManagementFactory.newPlatformMXBeanProxy[T](connection, objectName, manifest.runtimeClass.asInstanceOf[Class[T]])
  }
}