package com.redmellon.ingest

import com.redmellon.auth.{
  AngelOneSessionManager,
  SessionManager,
  SessionManagerFactory,
  SessionRegistry,
  ZerodhaSessionManager
}
import com.redmellon.common.util.Logging
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.{Bean, Configuration}

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

}
