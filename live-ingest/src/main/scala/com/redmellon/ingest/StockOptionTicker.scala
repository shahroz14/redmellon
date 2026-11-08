package com.redmellon.ingest

import com.redmellon.broker.model.ApiSession
import com.redmellon.common.BrokerName
import com.redmellon.common.model.{Exchange, Instrument, OptionSymbol, SubscribeMode}
import com.redmellon.common.util.Logging
import org.springframework.stereotype.Component

import com.redmellon.broker.session.SessionManagerFactory

import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters._

@Component
class StockOptionTicker(props: IngestProperties, factory: SessionManagerFactory, tickLogger: TickLogger)
    extends Logging {

  /** Subscribes each configured stock's spot and, on its first tick, the options within the configured strike range for
    * the next expiries. Safe to call again after a session refresh: the state is per call, as the ticker is new.
    */
  def subscribe(): Unit = {
    val symbols = props.stockOptionSymbols.asScala.map(_.trim).filter(_.nonEmpty).distinct.toSeq
    if (symbols.isEmpty) {
      logger.warn("No stock option symbols configured — skipping stock option ticker")
    } else if (!props.accounts.asScala.exists(_.broker == BrokerName.Zerodha)) {
      logger.warn("No Zerodha account configured — skipping stock option ticker")
    } else
      try {
        val session = factory.getSession(props.stockOptionAppName)
        val equities = session.appConnect.instruments(Exchange.NSE).filter(_.instrumentType == "EQ")
        val options = session.appConnect.instruments(Exchange.NFO)
        val subscribed = ConcurrentHashMap.newKeySet[String]()

        symbols.foreach { symbol =>
          equities.find(_.tradingSymbol == symbol) match {
            case None => logger.warn(s"Stock $symbol not found in NSE instruments — skipping")
            case Some(stock) =>
              session.appTicker.subscribe(
                Seq(stock.instrumentToken),
                SubscribeMode.Full,
                ticks => {
                  ticks.foreach(t => tickLogger.log(t.copy(symbol = symbol, exchangeToken = stock.exchangeToken)))
                  // Wait for a real price (it is 0 before the open) and subscribe the options only once.
                  ticks.find(_.lastPrice > 0).foreach { t =>
                    if (subscribed.add(symbol)) subscribeOptions(symbol, t.lastPrice, options, session)
                  }
                }
              )
          }
        }
        info(s"Subscribed ${symbols.size} stocks for option ticking: ${symbols.mkString(", ")}")
      } catch {
        case e: Exception => logger.error("Stock option ticker setup failed", e)
      }
  }

  private def subscribeOptions(symbol: String, spot: Double, options: Seq[Instrument], session: ApiSession): Unit = {
    val contracts = StockOptionSelector.select(
      options,
      symbol,
      spot,
      LocalDate.now(),
      props.stockOptionExpiryCount,
      props.stockOptionRangePercent
    )
    if (contracts.isEmpty) {
      logger.warn(s"No $symbol options found for spot $spot")
    } else {
      val tokens = contracts.map { c =>
        val normalized =
          OptionSymbol.parse(c.tradingSymbol, c.expiry).map(_.toNormalizedName).getOrElse(c.tradingSymbol)
        c.instrumentToken -> (normalized, c.exchangeToken)
      }.toMap
      val expiries = contracts.flatMap(StockOptionSelector.expiryDate).distinct.sorted
      info(s"Subscribing to ${tokens.size} $symbol options (spot $spot, expiries ${expiries.mkString(", ")})")
      session.appTicker.subscribe(
        tokens.keys.toSeq,
        SubscribeMode.Full,
        ticks =>
          ticks.foreach { t =>
            val (sym, exToken) = tokens(t.instrumentToken)
            tickLogger.log(t.copy(symbol = sym, exchangeToken = exToken))
          }
      )
    }
  }
}
