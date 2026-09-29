package com.redmellon.auth

import com.redmellon.common.BrokerName

class SessionManagerFactory(managers: List[SessionManager]) {

  private val registry: Map[BrokerName, SessionManager] =
    managers.map(m => m.brokerName -> m).toMap

  def forBroker(broker: BrokerName): SessionManager =
    registry.getOrElse(broker, throw new IllegalArgumentException(s"No SessionManager registered for broker: $broker"))
}
