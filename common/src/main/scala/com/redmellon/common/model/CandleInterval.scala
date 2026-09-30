package com.redmellon.common.model

sealed trait CandleInterval

object CandleInterval {
  case object OneMinute      extends CandleInterval
  case object ThreeMinutes   extends CandleInterval
  case object FiveMinutes    extends CandleInterval
  case object TenMinutes     extends CandleInterval
  case object FifteenMinutes extends CandleInterval
  case object ThirtyMinutes  extends CandleInterval
  case object OneHour        extends CandleInterval
  case object OneDay         extends CandleInterval
  case object OneWeek        extends CandleInterval
  case object OneMonth       extends CandleInterval
}
