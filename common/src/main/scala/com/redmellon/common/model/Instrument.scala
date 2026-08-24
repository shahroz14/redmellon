package com.redmellon.common.model

import com.redmellon.common.BrokerName

case class Instrument(
  instrumentToken: Long,
  exchangeToken: Long,
  tradingSymbol: String,
  name: String,
  exchange: String,
  segment: String,
  instrumentType: String,
  lastPrice: Double,
  tickSize: Double,
  lotSize: Int,
  strike: String,
  expiry: Option[Long],
  brokerType: BrokerName
)
