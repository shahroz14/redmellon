package com.redmellon.broker.api

import com.redmellon.common.model.{DepthLevel, MarketDepth, SubscribeMode, Tick}
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException
import com.zerodhatech.ticker.{KiteTicker, OnError}

import java.time.ZoneId
import scala.collection.mutable
import scala.jdk.CollectionConverters._

class ZerodhaTicker(ticker: KiteTicker) extends AppTicker {

  private val ist = ZoneId.of("Asia/Kolkata")

  // token -> (kiteMode, onTick callback)
  private val subscriptions = mutable.Map.empty[Long, (String, Seq[Tick] => Unit)]
  private var connected = false
  private var connectInitiated = false

  override def disconnect(): Unit = {
    ticker.setTryReconnection(false)
    ticker.disconnect()
    connected = false
  }

  def subscribe(tokens: Seq[Long], mode: SubscribeMode, onTick: Seq[Tick] => Unit): Unit = {
    val kiteMode = mode match {
      case SubscribeMode.LTP   => KiteTicker.modeLTP
      case SubscribeMode.Quote => KiteTicker.modeQuote
      case SubscribeMode.Full  => KiteTicker.modeFull
    }

    tokens.foreach(t => subscriptions(t) = (kiteMode, onTick))

    if (connected) {
      // Already connected — subscribe new tokens directly
      val tokenList = new java.util.ArrayList[java.lang.Long](tokens.map(Long.box).asJava)
      ticker.subscribe(tokenList)
      ticker.setMode(tokenList, kiteMode)
    } else if (!connectInitiated) {
      // First call — wire up all listeners and connect once
      connectInitiated = true

      ticker.setOnConnectedListener { () =>
        connected = true
        subscriptions.groupBy { case (_, (m, _)) => m }.foreach { case (m, entries) =>
          val tokenList = new java.util.ArrayList[java.lang.Long](entries.keys.map(Long.box).toSeq.asJava)
          ticker.subscribe(tokenList)
          ticker.setMode(tokenList, m)
        }
      }

      ticker.setOnTickerArrivalListener(ticks =>
        ticks.asScala.foreach { t =>
          subscriptions.get(t.getInstrumentToken).foreach { case (_, onTick) =>
            val depthOpt = Option(t.getMarketDepth).map { d =>
              def levels(key: String) =
                Option(d.get(key))
                  .map(_.asScala.map(l => DepthLevel(l.getPrice, l.getQuantity, l.getOrders)).toSeq)
                  .getOrElse(Seq.empty)
              MarketDepth(buy = levels("buy"), sell = levels("sell"))
            }
            onTick(Seq(Tick(
              instrumentToken = t.getInstrumentToken,
              mode = t.getMode,
              tradable = t.isTradable,
              symbol = "",
              lastPrice = t.getLastTradedPrice,
              lastTradedQuantity = t.getLastTradedQuantity,
              vwap = t.getAverageTradePrice,
              volume = t.getVolumeTradedToday,
              totalBuyQuantity = t.getTotalBuyQuantity,
              totalSellQuantity = t.getTotalSellQuantity,
              open = t.getOpenPrice,
              high = t.getHighPrice,
              low = t.getLowPrice,
              close = t.getClosePrice,
              change = t.getChange,
              openInterest = t.getOi,
              oiDayHigh = t.getOpenInterestDayHigh,
              oiDayLow = t.getOpenInterestDayLow,
              lastTradedTime = Option(t.getLastTradedTime).map(_.toInstant.atZone(ist).toLocalDateTime),
              tickTimestamp = Option(t.getTickTimestamp).map(_.toInstant.atZone(ist).toLocalDateTime),
              depth = depthOpt
            )))
          }
        }
      )

      ticker.setOnDisconnectedListener(() => connected = false)

      ticker.setOnErrorListener(new OnError {
        override def onError(e: Exception): Unit     = ()
        override def onError(e: KiteException): Unit = ()
        override def onError(msg: String): Unit      = ()
      })

      ticker.connect()
    }
    // else: connectInitiated but not yet connected — tokens registered, will be subscribed in onConnected
  }
}
