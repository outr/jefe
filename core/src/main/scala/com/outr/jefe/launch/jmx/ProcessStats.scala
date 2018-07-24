package com.outr.jefe.launch.jmx

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
