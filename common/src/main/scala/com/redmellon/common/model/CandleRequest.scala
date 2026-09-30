package com.redmellon.common.model

import java.time.LocalDateTime

case class CandleRequest(
  exchange: String,
  symbolToken: String,
  interval: CandleInterval,
  from: LocalDateTime,
  to: LocalDateTime
)
