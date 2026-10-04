package com.redmellon.common.model

import java.time.LocalDateTime

case class DepthLevel(price: Double, quantity: Int, orders: Int)
case class MarketDepth(buy: Seq[DepthLevel], sell: Seq[DepthLevel])

case class Tick(
  instrumentToken: Long,
  mode: String,
  tradable: Boolean,
  symbol: String,
  lastPrice: Double,
  lastTradedQuantity: Double,
  vwap: Double,
  volume: Long,
  totalBuyQuantity: Double,
  totalSellQuantity: Double,
  open: Double,
  high: Double,
  low: Double,
  close: Double,
  change: Double,
  openInterest: Double,
  oiDayHigh: Double,
  oiDayLow: Double,
  lastTradedTime: Option[LocalDateTime],
  tickTimestamp: Option[LocalDateTime],
  depth: Option[MarketDepth]
)
