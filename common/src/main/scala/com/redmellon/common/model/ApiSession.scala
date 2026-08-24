package com.redmellon.common.model

import com.redmellon.common.BrokerName

case class ApiSession(
  userId: String,
  userName: String,
  email: String,
  accessToken: String,
  publicToken: String,
  loginTime: String,
  brokerType: BrokerName,
  apiKey: String
)
