package com.redmellon.auth

import com.angelbroking.smartapi.SmartConnect
import com.redmellon.common.model.{Candle, CandleInterval, CandleRequest, Exchange, Instrument}
import org.json.JSONObject

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SmartConnectAdapter(sc: SmartConnect) extends ApiConnect {

  private val requestFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
  private val responseFormat = DateTimeFormatter.ISO_OFFSET_DATE_TIME

  def candleData(request: CandleRequest): Seq[Candle] = {
    val req = new JSONObject()
    req.put("exchange", request.exchange)
    req.put("symboltoken", request.symbolToken)
    req.put("interval", toAngelOneInterval(request.interval))
    req.put("fromdate", request.from.format(requestFormat))
    req.put("todate", request.to.format(requestFormat))
    val arr = sc.candleData(req)
    if (arr == null)
      throw new RuntimeException("candleData returned null — AngelOne API error (check logs above)")
    (0 until arr.length).map { i =>
      val row = arr.getJSONArray(i)
      Candle(
        timestamp = LocalDateTime.parse(row.getString(0), responseFormat),
        open = row.getDouble(1),
        high = row.getDouble(2),
        low = row.getDouble(3),
        close = row.getDouble(4),
        volume = row.getLong(5)
      )
    }.toSeq
  }

  def instruments(exchange: Exchange): Seq[Instrument] =
    throw new UnsupportedOperationException("AngelOne instrument listing not yet implemented")

  private def toAngelOneInterval(interval: CandleInterval): String = interval match {
    case CandleInterval.OneMinute      => "ONE_MINUTE"
    case CandleInterval.ThreeMinutes   => "THREE_MINUTE"
    case CandleInterval.FiveMinutes    => "FIVE_MINUTE"
    case CandleInterval.TenMinutes     => "TEN_MINUTE"
    case CandleInterval.FifteenMinutes => "FIFTEEN_MINUTE"
    case CandleInterval.ThirtyMinutes  => "THIRTY_MINUTE"
    case CandleInterval.OneHour        => "ONE_HOUR"
    case CandleInterval.OneDay         => "ONE_DAY"
    case CandleInterval.OneWeek        => "ONE_WEEK"
    case CandleInterval.OneMonth       => "ONE_MONTH"
  }
}
