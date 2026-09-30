package com.redmellon.auth

import com.redmellon.common.BrokerName

class SessionManagerFactory(managers: List[SessionManager]) {

  private val registry: Map[BrokerName, SessionManager] =
    managers.map(m => m.brokerName -> m).toMap

  def forBroker(broker: BrokerName): SessionManager =
    registry.getOrElse(broker, throw new IllegalArgumentException(s"No SessionManager registered for broker: $broker"))

  def getApiConnect(userId: String, apiKey: String): ApiConnect =
    registry.values
      .flatMap(_.getApiConnect(userId, apiKey))
      .headOption
      .getOrElse(throw new IllegalStateException(
        s"No active session for user $userId / app $apiKey — call login() first"
      ))
}
