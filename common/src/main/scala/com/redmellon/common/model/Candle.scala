package com.redmellon.common.model

import java.time.LocalDateTime

case class Candle(
  timestamp: LocalDateTime,
  open: Double,
  high: Double,
  low: Double,
  close: Double,
  volume: Long
)
