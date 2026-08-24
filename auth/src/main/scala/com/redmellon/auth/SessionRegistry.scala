package com.redmellon.auth

import com.redmellon.common.model.ApiSession

import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters._

class SessionRegistry {
  private val sessions = new ConcurrentHashMap[String, ApiSession]()

  private def key(userId: String, apiKey: String): String = s"$userId:$apiKey"
  def set(session: ApiSession): Unit                      = sessions.put(key(session.userId, session.apiKey), session)

  def get(userId: String, apiKey: String): Option[ApiSession]     = Option(sessions.get(key(userId, apiKey)))
  def remove(userId: String, apiKey: String): Unit                = sessions.remove(key(userId, apiKey))
  def accessToken(userId: String, apiKey: String): Option[String] = get(userId, apiKey).map(_.accessToken)
  def all: Map[String, ApiSession]                                = sessions.asScala.toMap
}
