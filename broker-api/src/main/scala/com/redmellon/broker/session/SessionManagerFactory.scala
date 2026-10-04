package com.redmellon.broker.session

import com.redmellon.auth.{AccountCredentials, AppCredentials, AppNotLoggedInException}
import com.redmellon.broker.model.ApiSession
import com.redmellon.common.BrokerName
import com.redmellon.common.util.Logging

import java.util.concurrent.ConcurrentHashMap

class SessionManagerFactory(managers: List[SessionManager]) extends Logging {

  private val brokerRegistry: Map[BrokerName, SessionManager] =
    managers.map(m => m.brokerName -> m).toMap

  private val sessions = new ConcurrentHashMap[String, ApiSession]()

  def login(credentials: AccountCredentials, app: AppCredentials): ApiSession = {
    val manager = brokerRegistry.getOrElse(
      credentials.broker,
      throw new IllegalArgumentException(s"No SessionManager registered for broker: ${credentials.broker}")
    )
    val session = manager.login(credentials, app)
    sessions.put(session.appName, session)
    session
  }

  def getSession(appName: String): ApiSession =
    Option(sessions.get(appName)).getOrElse(throw new AppNotLoggedInException(appName))

  def invalidateSession(appName: String): Unit =
    Option(sessions.get(appName)) match {
      case Some(session) =>
        try {
          session.appConnect.invalidate()
          info(s"session invalidated for app $appName (user ${session.userId})")
        } catch {
          case e: Exception =>
            logger.error(s"Couldn't invalidate session for app $appName", e)
        } finally
          sessions.remove(appName)
      case None =>
        logger.warn(s"No active session for app $appName")
    }
}
