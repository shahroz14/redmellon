package com.redmellon.broker.session

import com.redmellon.auth.{AccountCredentials, AppCredentials, ZerodhaLoginUtils}
import com.redmellon.broker.api.{ZerodhaConnect, ZerodhaTicker}
import com.redmellon.broker.model.ApiSession
import com.redmellon.common.BrokerName
import com.redmellon.common.util.Logging
import com.zerodhatech.kiteconnect.KiteConnect
import com.zerodhatech.ticker.KiteTicker

class ZerodhaSessionManager extends SessionManager with Logging {

  def login(credentials: AccountCredentials, app: AppCredentials): ApiSession = {
    val requestToken = ZerodhaLoginUtils.getRequestToken(credentials, app.apiKey)
    val kite = new KiteConnect(app.apiKey)
    val user = kite.generateSession(requestToken, app.apiSecret)
    info(
      s"session generated for user ${user.userId} (${user.userName}, ${user.email}) and app ${app.appName} on $brokerName"
    )
    kite.setAccessToken(user.accessToken)
    kite.setPublicToken(user.publicToken)

    val ticker = new KiteTicker(user.accessToken, app.apiKey)
    ticker.setTryReconnection(true)
    ticker.setMaximumRetries(10)
    ticker.setMaximumRetryInterval(30)

    ApiSession(
      userId = user.userId,
      userName = user.userName,
      email = user.email,
      accessToken = user.accessToken,
      publicToken = user.publicToken,
      loginTime = if (user.loginTime != null) user.loginTime.toString else "",
      brokerType = BrokerName.Zerodha,
      apiKey = app.apiKey,
      appName = app.appName,
      appConnect = new ZerodhaConnect(kite),
      appTicker = new ZerodhaTicker(ticker)
    )
  }

  override def brokerName: BrokerName = BrokerName.Zerodha
}
