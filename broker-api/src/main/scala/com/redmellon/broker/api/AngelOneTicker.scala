package com.redmellon.broker.api

import com.angelbroking.smartapi.smartstream.models._
import com.angelbroking.smartapi.smartstream.ticker.{SmartStreamListener, SmartStreamTicker}
import com.redmellon.common.model.{DepthLevel, MarketDepth, SubscribeMode, Tick}

import scala.jdk.CollectionConverters._

class AngelOneTicker(userId: String, feedToken: String) extends AppTicker {

  def subscribe(tokens: Seq[Long], mode: SubscribeMode, onTick: Seq[Tick] => Unit): Unit = {
    val smartMode = mode match {
      case SubscribeMode.LTP   => SmartStreamSubsMode.LTP
      case SubscribeMode.Quote => SmartStreamSubsMode.QUOTE
      case SubscribeMode.Full  => SmartStreamSubsMode.SNAP_QUOTE
    }

    val tokenIdSet = tokens.map(t => new TokenID(ExchangeType.NSE_CM, t.toString)).toSet.asJava

    var ticker: SmartStreamTicker = null

    val listener = new SmartStreamListener {
      override def onConnected(): Unit =
        ticker.subscribe(smartMode, new java.util.HashSet(tokenIdSet))

      override def onLTPArrival(ltp: LTP): Unit =
        onTick(Seq(toTickFromLtp(ltp)))

      override def onQuoteArrival(quote: Quote): Unit =
        onTick(Seq(toTickFromQuote(quote)))

      override def onSnapQuoteArrival(snapQuote: SnapQuote): Unit =
        onTick(Seq(toTickFromSnapQuote(snapQuote)))

      override def onDepthArrival(depth: Depth): Unit     = ()
      override def onDisconnected(): Unit                 = ()
      override def onError(error: SmartStreamError): Unit = ()
      override def onErrorCustom(): SmartStreamError      = null
      override def onPong(): Unit                         = ()
    }

    ticker = new SmartStreamTicker(userId, feedToken, listener, Int.MaxValue, Int.MaxValue)
    ticker.connect()
  }

  private def toTickFromLtp(ltp: LTP): Tick =
    Tick(
      instrumentToken = extractToken(ltp.getToken),
      mode = "ltp",
      tradable = true,
      symbol = "",
      lastPrice = ltp.getLastTradedPrice / 100.0,
      lastTradedQuantity = 0,
      vwap = 0,
      volume = 0,
      totalBuyQuantity = 0,
      totalSellQuantity = 0,
      open = 0,
      high = 0,
      low = 0,
      close = 0,
      change = 0,
      openInterest = 0,
      oiDayHigh = 0,
      oiDayLow = 0,
      lastTradedTime = None,
      tickTimestamp = None,
      depth = None
    )

  private def toTickFromQuote(quote: Quote): Tick =
    Tick(
      instrumentToken = extractToken(quote.getToken),
      mode = "quote",
      tradable = true,
      symbol = "",
      lastPrice = quote.getLastTradedPrice / 100.0,
      lastTradedQuantity = 0,
      vwap = quote.getAvgTradedPrice / 100.0,
      volume = quote.getVolumeTradedToday,
      totalBuyQuantity = quote.getTotalBuyQty,
      totalSellQuantity = quote.getTotalSellQty,
      open = quote.getOpenPrice / 100.0,
      high = quote.getHighPrice / 100.0,
      low = quote.getLowPrice / 100.0,
      close = quote.getClosePrice / 100.0,
      change = 0,
      openInterest = 0,
      oiDayHigh = 0,
      oiDayLow = 0,
      lastTradedTime = None,
      tickTimestamp = None,
      depth = None
    )

  private def toTickFromSnapQuote(snapQuote: SnapQuote): Tick = {
    val buyLevels = Option(snapQuote.getBestFiveBuy).map(_.map(dp =>
      DepthLevel(dp.getPrice / 100.0, dp.getQuantity.toInt, dp.getNumberOfOrders)
    ).toSeq).getOrElse(Seq.empty)
    val sellLevels = Option(snapQuote.getBestFiveSell).map(_.map(dp =>
      DepthLevel(dp.getPrice / 100.0, dp.getQuantity.toInt, dp.getNumberOfOrders)
    ).toSeq).getOrElse(Seq.empty)
    val depthOpt =
      if (buyLevels.nonEmpty || sellLevels.nonEmpty) Some(MarketDepth(buy = buyLevels, sell = sellLevels))
      else None

    Tick(
      instrumentToken = extractToken(snapQuote.getToken),
      mode = "full",
      tradable = true,
      symbol = "",
      lastPrice = snapQuote.getLastTradedPrice / 100.0,
      lastTradedQuantity = 0,
      vwap = snapQuote.getAvgTradedPrice / 100.0,
      volume = snapQuote.getVolumeTradedToday,
      totalBuyQuantity = snapQuote.getTotalBuyQty,
      totalSellQuantity = snapQuote.getTotalSellQty,
      open = snapQuote.getOpenPrice / 100.0,
      high = snapQuote.getHighPrice / 100.0,
      low = snapQuote.getLowPrice / 100.0,
      close = snapQuote.getClosePrice / 100.0,
      change = 0,
      openInterest = snapQuote.getOpenInterest.toDouble,
      oiDayHigh = 0,
      oiDayLow = 0,
      lastTradedTime = None,
      tickTimestamp = None,
      depth = depthOpt
    )
  }

  private def extractToken(tokenId: TokenID): Long =
    tokenId.getToken.filter(_.isDigit).toLong
}
