package com.redmellon.ingest

import com.redmellon.auth.AccountCredentials
import com.redmellon.common.BrokerName
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

import scala.beans.{BeanProperty, BooleanBeanProperty}

@Component
@ConfigurationProperties(prefix = "ingest")
class IngestProperties {
  @BeanProperty var storageBaseDir: String = _
  @BooleanBeanProperty var autoLogin: Boolean = true
  @BeanProperty var accounts: java.util.List[AccountCredentials] = new java.util.ArrayList()
}
