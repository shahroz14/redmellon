package com.redmellon.ingest

import com.redmellon.broker.session.SessionManagerFactory
import com.redmellon.common.util.Logging
import org.springframework.scheduling.annotation.{EnableScheduling, Scheduled}
import org.springframework.stereotype.Component

@Component
@EnableScheduling
class SessionRefreshScheduler(props: IngestProperties, factory: SessionManagerFactory, niftyTicker: NiftyTicker)
    extends Logging {

  // Kite tokens expire daily, so log every app in again before market open and re-attach the ticker.
  @Scheduled(cron = "0 0 9 * * MON-FRI", zone = "Asia/Kolkata")
  def refreshSessions(): Unit =
    if (props.autoLogin) {
      info("Refreshing broker sessions")
      val results = factory.refreshAll(props.loginMaxAttempts, props.loginRetryDelaySeconds * 1000L)
      results.foreach { case (appName, session) =>
        if (session.isEmpty) logger.error(s"Session refresh failed for app $appName")
      }
      if (results.get("redmellon_premium").exists(_.isDefined)) niftyTicker.subscribe()
    }
}
