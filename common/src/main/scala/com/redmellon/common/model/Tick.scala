package com.redmellon.common.model

import com.redmellon.common.disk.CsvConvertible

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

case class DepthLevel(price: Double, quantity: Int, orders: Int)
case class MarketDepth(buy: Seq[DepthLevel], sell: Seq[DepthLevel])

case class Tick(
  instrumentToken: Long,
  exchangeToken: Long = 0,
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

object Tick {
  private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

  implicit val TickCsvConverter: CsvConvertible[Tick] = new CsvConvertible[Tick] {
    override def toHeader: String =
      "Timestamp,Token,Name,Ltp,Vwap,Volume,OpenInterest,High,Low,Tbq,Tsq,SpotPrice,BestFiveBuy,BestFiveSell"

    override def toCsvRow(t: Tick): String = {
      val ts = t.tickTimestamp.map(_.format(fmt)).getOrElse(LocalDateTime.now().format(fmt))
      val buy = t.depth.map(_.buy.map(d => s"${d.orders}/${d.quantity}/${d.price}").mkString("; ")).getOrElse("")
      val sell = t.depth.map(_.sell.map(d => s"${d.orders}/${d.quantity}/${d.price}").mkString("; ")).getOrElse("")
      s"$ts,${t.exchangeToken},${t.symbol},${t.lastPrice},${t.vwap},${t.volume},${t.openInterest},${t.high},${t.low},${t.totalBuyQuantity},${t.totalSellQuantity},-1.0,$buy,$sell"
    }

    override def fromCsv(values: String*): Tick =
      Tick(
        instrumentToken = values(1).toLong,
        mode = "full",
        tradable = true,
        symbol = values(2),
        lastPrice = values(3).toDouble,
        lastTradedQuantity = 0,
        vwap = values(4).toDouble,
        volume = values(5).toLong,
        totalBuyQuantity = values(9).toDouble,
        totalSellQuantity = values(10).toDouble,
        open = 0,
        high = values(7).toDouble,
        low = values(8).toDouble,
        close = 0,
        change = 0,
        openInterest = values(6).toDouble,
        oiDayHigh = 0,
        oiDayLow = 0,
        lastTradedTime = None,
        tickTimestamp = Some(LocalDateTime.parse(values(0), fmt)),
        depth = None
      )
  }
}
