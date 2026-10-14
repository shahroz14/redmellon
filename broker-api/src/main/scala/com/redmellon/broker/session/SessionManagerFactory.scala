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
  private val credsByAppName = new ConcurrentHashMap[String, (AccountCredentials, AppCredentials)]()

  def login(credentials: AccountCredentials, app: AppCredentials): ApiSession = {
    val manager = brokerRegistry.getOrElse(
      credentials.broker,
      throw new IllegalArgumentException(s"No SessionManager registered for broker: ${credentials.broker}")
    )
    val session = manager.login(credentials, app)
    sessions.put(session.appName, session)
    credsByAppName.put(session.appName, (credentials, app))
    session
  }

  def loginWithRetry(
    credentials: AccountCredentials,
    app: AppCredentials,
    maxAttempts: Int,
    delayMs: Long
  ): Option[ApiSession] = {
    var attempt = 1
    while (attempt <= maxAttempts) {
      try
        return Some(login(credentials, app))
      catch {
        case e: Exception =>
          logger.warn(s"Login attempt $attempt/$maxAttempts failed for app ${app.appName}: ${e.getMessage}")
          if (attempt < maxAttempts) Thread.sleep(delayMs)
      }
      attempt += 1
    }
    logger.error(s"Login failed for app ${app.appName} after $maxAttempts attempts")
    None
  }

  /** Re-logins every app that has logged in before. Returns the new session per app (None if all attempts failed). */
  def refreshAll(maxAttempts: Int, delayMs: Long): Map[String, Option[ApiSession]] = {
    import scala.jdk.CollectionConverters._
    credsByAppName.asScala.toList.map { case (appName, (credentials, app)) =>
      Option(sessions.get(appName)).foreach { old =>
        try old.appTicker.disconnect()
        catch { case e: Exception => logger.warn(s"Couldn't disconnect ticker for app $appName", e) }
      }
      appName -> loginWithRetry(credentials, app, maxAttempts, delayMs)
    }.toMap
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
