package com.redmellon.broker.api

import com.redmellon.common.BrokerName.Zerodha
import com.redmellon.common.model.{Exchange, Instrument}
import com.zerodhatech.kiteconnect.KiteConnect

import scala.jdk.CollectionConverters.CollectionHasAsScala

class ZerodhaConnect(kite: KiteConnect) extends AppConnect {
  def instruments(exchange: Exchange): Seq[Instrument] = {
    val instruments = kite.getInstruments(exchange.toString)
    instruments.asScala.map { instrument =>
      Instrument(
        instrument.instrument_token,
        instrument.exchange_token,
        instrument.tradingsymbol,
        instrument.name,
        exchange.toString,
        instrument.segment,
        instrument.instrument_type,
        instrument.last_price,
        instrument.tick_size,
        instrument.lot_size,
        instrument.strike,
        Option(instrument.expiry).map(_.getTime),
        Zerodha
      )
    }.toSeq
  }

  override def invalidate(): Unit = ()
}
