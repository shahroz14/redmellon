package com.redmellon.ingest

import com.redmellon.broker.session.{
  AngelOneSessionManager,
  SessionManager,
  SessionManagerFactory,
  ZerodhaSessionManager
}
import com.redmellon.common.BrokerName.Zerodha
import com.redmellon.common.util.Logging
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.core.annotation.Order

import java.time.{DayOfWeek, Duration, ZoneId, ZonedDateTime}
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
    val todayOpen = now.withHour(9).withMinute(14).withSecond(30).withNano(0)
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
              factory.loginWithRetry(account, app, props.loginMaxAttempts, props.loginRetryDelaySeconds * 1000L)
            }
          }
        }
      }

  @Bean
  @Order(4)
  def niftyTickerRunner(niftyTicker: NiftyTicker): ApplicationRunner =
    _ => niftyTicker.subscribe()
}
