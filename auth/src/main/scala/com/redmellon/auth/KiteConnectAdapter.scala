package com.redmellon.auth

import com.redmellon.common.BrokerName
import com.redmellon.common.model.{Candle, CandleInterval, CandleRequest, Exchange, Instrument}
import com.zerodhatech.kiteconnect.KiteConnect

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId}
import java.util.Date
import scala.jdk.CollectionConverters._

class KiteConnectAdapter(kite: KiteConnect) extends ApiConnect {

  private val tsFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
  private val dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

  def candleData(request: CandleRequest): Seq[Candle] = {
    val from = toDate(request.from)
    val to = toDate(request.to)
    kite
      .getHistoricalData(from, to, request.symbolToken, toKiteInterval(request.interval), false, false)
      .dataArrayList
      .asScala
      .map { h =>
        Candle(
          timestamp = LocalDateTime.parse(h.timeStamp, tsFormat),
          open = h.open,
          high = h.high,
          low = h.low,
          close = h.close,
          volume = h.volume
        )
      }
      .toSeq
  }

  def instruments(exchange: Exchange): Seq[Instrument] =
    kite
      .getInstruments(toKiteExchange(exchange))
      .asScala
      .map { i =>
        Instrument(
          instrumentToken = i.instrument_token,
          exchangeToken = i.exchange_token,
          tradingSymbol = i.tradingsymbol,
          name = i.name,
          exchange = i.exchange,
          segment = i.segment,
          instrumentType = i.instrument_type,
          lastPrice = i.last_price,
          tickSize = i.tick_size,
          lotSize = i.lot_size,
          strike = i.strike,
          expiry = Option(i.expiry).map(_.getTime),
          brokerType = BrokerName.Zerodha
        )
      }
      .toSeq

  private def toDate(dt: LocalDateTime): Date =
    Date.from(dt.atZone(ZoneId.systemDefault()).toInstant)

  private def toKiteExchange(exchange: Exchange): String = exchange match {
    case Exchange.NSE => "NSE"
    case Exchange.BSE => "BSE"
    case Exchange.NFO => "NFO"
    case Exchange.BFO => "BFO"
    case Exchange.CDS => "CDS"
    case Exchange.MCX => "MCX"
    case Exchange.BCD => "BCD"
  }

  private def toKiteInterval(interval: CandleInterval): String = interval match {
    case CandleInterval.OneMinute      => "minute"
    case CandleInterval.ThreeMinutes   => "3minute"
    case CandleInterval.FiveMinutes    => "5minute"
    case CandleInterval.TenMinutes     => "10minute"
    case CandleInterval.FifteenMinutes => "15minute"
    case CandleInterval.ThirtyMinutes  => "30minute"
    case CandleInterval.OneHour        => "60minute"
    case CandleInterval.OneDay         => "day"
    case CandleInterval.OneWeek        => "week"
    case CandleInterval.OneMonth       => "month"
  }
}
