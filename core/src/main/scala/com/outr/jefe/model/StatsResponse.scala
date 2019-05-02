package com.outr.jefe.model

import com.outr.jefe.launch.jmx.ApplicationStats
import io.youi.ValidationError

case class StatsResponse(stats: Option[ApplicationStats], errors: List[ValidationError])