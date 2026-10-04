package com.redmellon.broker.api

import com.redmellon.common.model.{Exchange, Instrument}

trait AppConnect {
  def instruments(exchange: Exchange): Seq[Instrument]
  def invalidate(): Unit
}
