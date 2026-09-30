package com.redmellon.auth

import com.redmellon.common.BrokerName
import com.redmellon.common.model.ApiSession

abstract class SessionManager {
  def brokerName: BrokerName
  def login(credentials: AccountCredentials, app: AppCredentials): ApiSession
  def invalidateSession(userId: String, apiKey: String): Unit
  def getApiConnect(userId: String, apiKey: String): Option[ApiConnect]
  def sessionKey(userId: String, apiKey: String) = s"$userId:$apiKey"
}
