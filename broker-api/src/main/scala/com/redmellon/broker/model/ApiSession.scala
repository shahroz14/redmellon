package com.redmellon.broker.model

import com.redmellon.broker.api.{AppConnect, AppTicker}
import com.redmellon.common.BrokerName

case class ApiSession(
  userId: String,
  userName: String,
  email: String,
  accessToken: String,
  publicToken: String,
  loginTime: String,
  brokerType: BrokerName,
  apiKey: String,
  appName: String,
  appConnect: AppConnect,
  appTicker: AppTicker
)
