package com.outr.jefe.model

import com.outr.jefe.launch.jmx.ProcessStats
import io.youi.ValidationError

case class StatsResponse(stats: Option[ProcessStats], errors: List[ValidationError])