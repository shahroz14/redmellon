package com.redmellon.ingest

import com.redmellon.auth.{
  AngelOneSessionManager,
  SessionManager,
  SessionManagerFactory,
  SessionRegistry,
  ZerodhaSessionManager
}
import com.redmellon.common.BrokerName
import com.redmellon.common.model.{CandleInterval, CandleRequest, Exchange}
import com.redmellon.common.util.Logging
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.core.annotation.Order

import java.time.LocalDateTime
import scala.jdk.CollectionConverters._

@Configuration
class LiveIngestBeans extends Logging {

  @Bean
  def sessionRegistry(): SessionRegistry = new SessionRegistry()

  @Bean
  def zerodhaSessionManager(sessionRegistry: SessionRegistry): ZerodhaSessionManager =
    new ZerodhaSessionManager(sessionRegistry)

  @Bean
  def angelOneSessionManager(sessionRegistry: SessionRegistry): AngelOneSessionManager =
    new AngelOneSessionManager(sessionRegistry)

  @Bean
  def sessionManagerFactory(managers: java.util.List[SessionManager]): SessionManagerFactory =
    new SessionManagerFactory(managers.asScala.toList)

  @Bean
  @Order(1)
  def autoLoginRunner(props: IngestProperties, factory: SessionManagerFactory): ApplicationRunner =
    _ =>
      if (props.autoLogin) {
        props.accounts.asScala.foreach { account =>
          val sessionManager = factory.forBroker(account.broker)
          account.apps.asScala.foreach { app =>
            info(s"Logging in user ${account.userId} for app ${app.apiKey}")
            sessionManager.login(account, app)
          }
        }
      }

  // TODO: temporary — remove after testing
  @Bean
  @Order(2)
  def nseInstrumentsRunner(
    props: IngestProperties,
    factory: SessionManagerFactory
  ): ApplicationRunner =
    _ => {
      val sessionOpt = for {
        acc <- props.accounts.asScala.find(_.broker == BrokerName.Zerodha)
        app <- acc.apps.asScala.headOption
      } yield (acc.userId, app.apiKey)

      sessionOpt match {
        case None =>
          logger.warn("No Zerodha session available — skipping NSE instruments fetch")
        case Some((userId, apiKey)) =>
          try {
            val instruments = factory.getApiConnect(userId, apiKey).instruments(Exchange.NFO)
            info(s"NSE instruments count: ${instruments.size}")
            instruments.take(5).foreach(i => info(s"  ${i.tradingSymbol} (${i.instrumentToken})"))
          } catch {
            case e: Exception => logger.error("NSE instruments fetch failed", e)
          }
      }
    }

  // TODO: temporary — remove after testing
  @Bean
  @Order(3)
  def relianceCandleRunner(
    props: IngestProperties,
    factory: SessionManagerFactory
  ): ApplicationRunner =
    _ => {
      val sessionOpt = for {
        acc <- props.accounts.asScala.find(_.broker == BrokerName.AngelOne)
        app <- acc.apps.asScala.headOption
      } yield (acc.userId, app.apiKey)

      sessionOpt match {
        case None =>
          logger.warn("No AngelOne session available — skipping Reliance candle fetch")
        case Some((userId, apiKey)) =>
          try {
            val req = CandleRequest(
              exchange = "NSE",
              symbolToken = "2885",
              interval = CandleInterval.OneDay,
              from = LocalDateTime.of(2025, 1, 1, 9, 15),
              to = LocalDateTime.of(2025, 1, 10, 15, 30)
            )
            val candles = factory.getApiConnect(userId, apiKey).candleData(req)
            info(s"Reliance 1D candles: $candles")
          } catch {
            case e: Exception => logger.error("Reliance candle fetch failed", e)
          }
      }
    }

}
