package com.redmellon.ingest

import com.redmellon.auth.AppNotLoggedInException
import com.redmellon.broker.model.ApiSession
import com.redmellon.broker.session.{
  AngelOneSessionManager,
  SessionManager,
  SessionManagerFactory,
  ZerodhaSessionManager
}
import com.redmellon.common.BrokerName
import com.redmellon.common.BrokerName.Zerodha
import com.redmellon.common.model.{Exchange, Instrument, OptionSymbol, SubscribeMode}
import com.redmellon.common.util.Logging
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.core.annotation.Order

import java.time.{DayOfWeek, Duration, Instant, LocalDate, ZoneId, ZonedDateTime}
import scala.jdk.CollectionConverters._

@Configuration
class LiveIngestBeans extends Logging {

  @Bean
  def zerodhaSessionManager(): ZerodhaSessionManager = new ZerodhaSessionManager()

  @Bean
  def angelOneSessionManager(): AngelOneSessionManager = new AngelOneSessionManager()

  @Bean
  def sessionManagerFactory(managers: java.util.List[SessionManager]): SessionManagerFactory =
    new SessionManagerFactory(managers.asScala.toList)

  @Bean
  def tickLogger(): TickLogger = new TickLogger()

  @Bean
  @Order(0)
  def marketHoursRunner(props: IngestProperties): ApplicationRunner =
    _ => if (!props.bypassMarketHours) waitForMarketOpen()

  private def waitForMarketOpen(): Unit = {
    val ist = ZoneId.of("Asia/Kolkata")
    val now = ZonedDateTime.now(ist)
    val todayOpen  = now.withHour(9).withMinute(14).withSecond(30).withNano(0)
    val todayClose = now.withHour(15).withMinute(30).withSecond(0).withNano(0)

    val isWeekday = now.getDayOfWeek != DayOfWeek.SATURDAY && now.getDayOfWeek != DayOfWeek.SUNDAY
    if (isWeekday && now.isAfter(todayOpen) && now.isBefore(todayClose)) return

    var nextOpen = if (now.isBefore(todayOpen)) todayOpen else todayOpen.plusDays(1)
    while (nextOpen.getDayOfWeek == DayOfWeek.SATURDAY || nextOpen.getDayOfWeek == DayOfWeek.SUNDAY)
      nextOpen = nextOpen.plusDays(1)

    val secsToWait = Duration.between(now, nextOpen).getSeconds
    info(s"Market is closed — waiting $secsToWait seconds until $nextOpen")
    Thread.sleep(secsToWait * 1000)
  }

  @Bean
  @Order(1)
  def autoLoginRunner(props: IngestProperties, factory: SessionManagerFactory): ApplicationRunner =
    _ =>
      if (props.autoLogin) {
        props.accounts.asScala.foreach { account =>
          account.apps.asScala.foreach { app =>
            if (account.broker == Zerodha) {
              info(s"Logging in user ${account.userId} for app ${app.appName}")
              factory.login(account, app)
            }
          }
        }
      }

  @Bean
  @Order(4)
  def niftyTickerRunner(
    props: IngestProperties,
    factory: SessionManagerFactory,
    tickLogger: TickLogger
  ): ApplicationRunner =
    _ => {
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

      def subscribeToNiftyFno(niftyPrice: Double, instruments: Seq[Instrument], session: ApiSession): Unit = {
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
}
