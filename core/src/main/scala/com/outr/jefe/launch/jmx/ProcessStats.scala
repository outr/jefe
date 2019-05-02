package com.outr.jefe.launch.jmx

case class ProcessStats(id: String,
                        os: OperatingSystem,
                        heapUsage: MemoryUsage,
                        nonHeapUsage: MemoryUsage,
                        threading: Threading,
                        classLoading: ClassLoading) {
  def toList: List[String] = List(
    s"ID: $id",
    s"OS: $os",
    s"Heap Usage: $heapUsage",
    s"Non Heap Usage: $nonHeapUsage",
    s"Threading: $threading",
    s"Class Loading: $classLoading"
  )

  override def toString: String = toList.mkString("\n")
}

case class ApplicationStats(name: String,
                            enabled: Boolean,
                            running: Boolean,
                            forked: Boolean,
                            process: Option[ProcessStats]) {
  override def toString: String = {
    val ps = process.map { lines =>
      lines.toList.map(s => s"\t\t$s").mkString("\n")
    }.getOrElse("\t\tNo process running")
    s"""$name
       |\tEnabled: $enabled
       |\tRunning: $running
       |\tForked: $forked
       |\tProcess:
       |$ps
     """.stripMargin
  }
}