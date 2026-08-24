package com.redmellon.ingest

import com.redmellon.auth.{SessionRegistry, ZerodhaSessionManager}
import org.springframework.context.annotation.{Bean, Configuration}

@Configuration
class LiveIngestBeans {

  @Bean
  def sessionRegistry(): SessionRegistry = new SessionRegistry()

  @Bean
  def zerodhaSessionManager(sessionRegistry: SessionRegistry): ZerodhaSessionManager =
    new ZerodhaSessionManager(sessionRegistry)

}
