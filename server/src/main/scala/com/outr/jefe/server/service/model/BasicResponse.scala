package com.outr.jefe.server.service.model

import io.youi.ValidationError

case class BasicResponse(success: Boolean, errors: List[ValidationError])