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
  @BeanProperty var loginMaxAttempts: Int = 5
  @BeanProperty var loginRetryDelaySeconds: Int = 30
  @BooleanBeanProperty var bypassMarketHours: Boolean = false
  // Each ticker can be switched off on its own. Both are on by default.
  @BooleanBeanProperty var niftyTickerEnabled: Boolean = true
  @BooleanBeanProperty var stockOptionTickerEnabled: Boolean = true
  // Stocks whose options are ticked. Defaults to seven heavy NIFTY 50 weights plus M&M and IOC; override in yml to change the list.
  @BeanProperty var stockOptionSymbols: java.util.List[String] = new java.util.ArrayList(
    java.util.Arrays.asList(
      "HDFCBANK",
      "ICICIBANK",
      "RELIANCE",
      "BHARTIARTL",
      "LT",
      "SBIN",
      "INFY",
      "M&M",
      "IOC"
    )
  )
  // Number of upcoming expiries (from today) to subscribe for each stock.
  @BeanProperty var stockOptionExpiryCount: Int = 2
  // Strikes within +/- this percentage of the stock's price are subscribed.
  @BeanProperty var stockOptionRangePercent: Double = 20.0
  @BeanProperty var stockOptionAppName: String = "redmellon_premium"
  @BeanProperty var accounts: java.util.List[AccountCredentials] = new java.util.ArrayList()
}
