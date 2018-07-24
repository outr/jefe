package com.outr.jefe.model

import io.youi.ValidationError

case class BasicResponse(success: Boolean, errors: List[ValidationError])