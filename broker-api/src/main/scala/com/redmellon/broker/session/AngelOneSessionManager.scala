package com.redmellon.broker.session

import com.angelbroking.smartapi.SmartConnect
import com.redmellon.auth.{AccountCredentials, AppCredentials, TotpGenerator}
import com.redmellon.broker.api.{AngelOneConnect, AngelOneTicker}
import com.redmellon.broker.model.ApiSession
import com.redmellon.common.BrokerName
import com.redmellon.common.util.Logging

class AngelOneSessionManager extends SessionManager with Logging {

  def login(credentials: AccountCredentials, app: AppCredentials): ApiSession = {
    val totpCode = TotpGenerator.generate(credentials.totpSecret)
    val smartConnect = new SmartConnect(app.apiKey)
    val user = smartConnect.generateSession(credentials.userId, credentials.password, totpCode)
    if (user == null)
      throw new RuntimeException(
        s"generateSession returned null for userId=${credentials.userId} — check credentials, TOTP, and network connectivity"
      )
    smartConnect.setUserId(user.userId)
    smartConnect.setAccessToken(user.accessToken)
    smartConnect.setRefreshToken(user.refreshToken)
    info(
      s"session generated for user ${user.userId} (${user.userName}, ${user.email}) and app ${app.appName} on $brokerName"
    )

    ApiSession(
      userId = user.userId,
      userName = user.userName,
      email = user.email,
      accessToken = user.accessToken,
      publicToken = user.feedToken,
      loginTime = if (user.lastLoginTime != null) user.lastLoginTime.toString else "",
      brokerType = BrokerName.AngelOne,
      apiKey = app.apiKey,
      appName = app.appName,
      appConnect = new AngelOneConnect(),
      appTicker = new AngelOneTicker(user.userId, user.feedToken)
    )
  }

  override def brokerName: BrokerName = BrokerName.AngelOne
}
