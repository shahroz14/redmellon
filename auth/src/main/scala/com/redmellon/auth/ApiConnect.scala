package com.redmellon.auth

import com.redmellon.common.model.{Candle, CandleRequest, Exchange, Instrument}

trait ApiConnect {
  def candleData(request: CandleRequest): Seq[Candle]
  def instruments(exchange: Exchange): Seq[Instrument]
}
