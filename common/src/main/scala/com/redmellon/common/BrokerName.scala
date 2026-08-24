package com.redmellon.common

sealed trait BrokerName

object BrokerName {
  case object Zerodha extends BrokerName
}
