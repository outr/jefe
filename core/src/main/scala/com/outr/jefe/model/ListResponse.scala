package com.outr.jefe.model

import com.outr.jefe.launch.jmx.ApplicationStats
import io.youi.ValidationError

case class ListResponse(stats: List[ApplicationStats] = Nil,
                        success: Boolean,
                        errors: List[ValidationError])