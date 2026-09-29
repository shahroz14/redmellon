package com.redmellon.auth

import com.angelbroking.smartapi.SmartConnect
import com.redmellon.common.BrokerName
import com.redmellon.common.model.ApiSession
import com.redmellon.common.util.Logging

import java.util.concurrent.ConcurrentHashMap

class AngelOneSessionManager(sessionRegistry: SessionRegistry) extends SessionManager with Logging {

  private val smartConnectMap = new ConcurrentHashMap[String, SmartConnect]()

  def login(credentials: AccountCredentials, app: AppCredentials): ApiSession = {
    val totpCode = TotpGenerator.generate(credentials.totpSecret)
    val smartConnect = smartConnectMap.computeIfAbsent(
      sessionKey(credentials.userId, app.apiKey),
      _ => new SmartConnect(app.apiKey)
    )
    val user = smartConnect.generateSession(credentials.userId, credentials.password, totpCode)
    if (user == null)
      throw new RuntimeException(
        s"generateSession returned null for userId=${credentials.userId} — check credentials, TOTP, and network connectivity"
      )
    smartConnect.setUserId(user.userId)
    smartConnect.setAccessToken(user.accessToken)
    smartConnect.setRefreshToken(user.refreshToken)
    info(
      s"session generated for user ${user.userId} (${user.userName}, ${user.email}) and app ${app.apiKey} on $brokerName"
    )
    val session = ApiSession(
      userId = user.userId,
      userName = user.userName,
      email = user.email,
      accessToken = user.accessToken,
      publicToken = user.feedToken,
      loginTime = if (user.lastLoginTime != null) user.lastLoginTime.toString else "",
      brokerType = BrokerName.AngelOne,
      apiKey = app.apiKey
    )
    sessionRegistry.set(session)
    session
  }

  def invalidateSession(userId: String, apiKey: String): Unit =
    sessionRegistry.get(userId, apiKey) match {
      case Some(_) =>
        try {
          val smartConnect = smartConnectMap.get(sessionKey(userId, apiKey))
          require(smartConnect != null, s"SmartConnect instance not found for user $userId and app $apiKey")
          smartConnect.logout()
          info(s"session invalidated for user $userId and app $apiKey on $brokerName")
        } catch {
          case e: Exception =>
            logger.error(s"Couldn't invalidate the session for user $userId and app $apiKey on $brokerName", e)
        } finally {
          sessionRegistry.remove(userId, apiKey)
          smartConnectMap.remove(sessionKey(userId, apiKey))
        }
      case None =>
        logger.warn(s"No active session for user $userId and app $apiKey on $brokerName")
    }

  override def brokerName: BrokerName = BrokerName.AngelOne
}
