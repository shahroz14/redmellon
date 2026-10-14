package com.redmellon.ingest

import com.redmellon.broker.model.ApiSession
import com.redmellon.broker.session.SessionManagerFactory
import com.redmellon.common.BrokerName
import com.redmellon.common.model.{Exchange, Instrument, OptionSymbol, SubscribeMode}
import com.redmellon.common.util.Logging
import org.springframework.stereotype.Component

import java.time.{Instant, LocalDate, ZoneId}
import scala.jdk.CollectionConverters._

@Component
class NiftyTicker(props: IngestProperties, factory: SessionManagerFactory, tickLogger: TickLogger) extends Logging {

  /** Subscribes NIFTY spot and, on its first tick, the surrounding NFO strikes on the current session. */
  def subscribe(): Unit =
    if (!props.accounts.asScala.exists(_.broker == BrokerName.Zerodha)) {
      logger.warn("No Zerodha account configured — skipping NIFTY ticker")
    } else
      try {
        val session = factory.getSession("redmellon_premium")

        val nseInstruments = session.appConnect.instruments(Exchange.NSE)
        val instruments = session.appConnect.instruments(Exchange.NFO)
        var subscribedFno = false

        nseInstruments.find(i => i.tradingSymbol == "NIFTY 50") match {
          case Some(nifty) =>
            info(s"NIFTY instrument: $nifty")
            session.appTicker.subscribe(
              Seq(nifty.instrumentToken),
              SubscribeMode.Full,
              ticks => {
                ticks.foreach { t =>
                  tickLogger.log(t.copy(symbol = "NIFTY", exchangeToken = nifty.exchangeToken))
                }
                if (!subscribedFno) {
                  subscribeToNiftyFno(ticks.head.lastPrice, instruments, session)
                  subscribedFno = true
                }
              }
            )
          case None =>
            logger.warn("NIFTY instrument not found in NFO instruments")
        }

      } catch {
        case e: Exception => logger.error("NIFTY ticker setup failed", e)
      }

  private def subscribeToNiftyFno(niftyPrice: Double, instruments: Seq[Instrument], session: ApiSession): Unit = {
    val ist = ZoneId.of("Asia/Kolkata")
    val lowerBound = niftyPrice - 1500
    val upperBound = niftyPrice + 1500
    val expiryUpperBound = LocalDate.now().plusDays(180)

    val tokens = instruments
      .filter { i =>
        val strikeInRange = i.strike.toDoubleOption.exists(s => s >= lowerBound && s <= upperBound)
        val expiryInRange =
          i.expiry.exists(ms => Instant.ofEpochMilli(ms).atZone(ist).toLocalDate.isBefore(expiryUpperBound))
        i.tradingSymbol.startsWith("NIFTY") && strikeInRange && expiryInRange
      }
      .map { e =>
        val normalizedSymbol =
          OptionSymbol.parse(e.tradingSymbol, e.expiry).map(_.toNormalizedName).getOrElse(e.tradingSymbol)
        (e.instrumentToken, (normalizedSymbol, e.exchangeToken))
      }.toMap

    info(s"Subscribing to ${tokens.size} NIFTY NFO instruments")

    session.appTicker
      .subscribe(
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
