package com.redmellon.broker.session

import com.redmellon.auth.{AccountCredentials, AppCredentials}
import com.redmellon.broker.model.ApiSession
import com.redmellon.common.BrokerName

abstract class SessionManager {
  def brokerName: BrokerName
  def login(credentials: AccountCredentials, app: AppCredentials): ApiSession
}
