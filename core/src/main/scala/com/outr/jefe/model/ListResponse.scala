package com.outr.jefe.model

import com.outr.jefe.launch.jmx.{ApplicationStats, ProcessStats}
import io.youi.ValidationError

case class ListResponse(stats: ProcessStats,
                        applicationStats: List[ApplicationStats] = Nil,
                        success: Boolean,
                        errors: List[ValidationError])