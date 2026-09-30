package com.redmellon.auth

import com.redmellon.common.BrokerName
import com.redmellon.common.model.ApiSession
import com.redmellon.common.util.Logging
import com.zerodhatech.kiteconnect.KiteConnect

import java.util.concurrent.ConcurrentHashMap

class ZerodhaSessionManager(sessionRegistry: SessionRegistry) extends SessionManager with Logging {

  private val kiteConnectMap = new ConcurrentHashMap[String, KiteConnect]()

  def login(credentials: AccountCredentials, app: AppCredentials): ApiSession = {
    val requestToken = ZerodhaLoginUtils.getRequestToken(credentials, app.apiKey)
    val kite =
      kiteConnectMap.computeIfAbsent(sessionKey(credentials.userId, app.apiKey), _ => new KiteConnect(app.apiKey))
    val user = kite.generateSession(requestToken, app.apiSecret)
    info(
      s"session generated for user ${user.userId} (${user.userName}, ${user.email}) and app ${app.apiKey} on $brokerName"
    )
    kite.setAccessToken(user.accessToken)
    kite.setPublicToken(user.publicToken)
    val session = ApiSession(
      userId = user.userId,
      userName = user.userName,
      email = user.email,
      accessToken = user.accessToken,
      publicToken = user.publicToken,
      loginTime = if (user.loginTime != null) user.loginTime.toString else "",
      brokerType = BrokerName.Zerodha,
      apiKey = app.apiKey
    )
    sessionRegistry.set(session)
    session
  }

  def getApiConnect(userId: String, apiKey: String): Option[ApiConnect] =
    Option(kiteConnectMap.get(sessionKey(userId, apiKey))).map(new KiteConnectAdapter(_))

  def invalidateSession(userId: String, apiKey: String): Unit =
    sessionRegistry.get(userId, apiKey) match {
      case Some(session) =>
        try {
          val kite = kiteConnectMap.get(sessionKey(userId, apiKey))
          require(kite != null, s"KiteConnect instance not found for user $userId and app $apiKey")
          kite.setAccessToken(session.accessToken)
          kite.invalidateAccessToken()
          info(s"session invalidated for user $userId and app $apiKey on $brokerName")
        } catch {
          case e: Exception =>
            logger.error(s"Couldn't invalidate the session for user $userId and app $apiKey on $brokerName", e)
        } finally {
          sessionRegistry.remove(userId, apiKey)
          kiteConnectMap.remove(sessionKey(userId, apiKey))
        }
      case None =>
        logger.warn(s"No active session for user $userId and app $apiKey on $brokerName")
    }

  override def brokerName: BrokerName = BrokerName.Zerodha
}
