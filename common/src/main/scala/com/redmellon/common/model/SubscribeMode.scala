package com.redmellon.common.model

sealed trait SubscribeMode
object SubscribeMode {
  case object LTP   extends SubscribeMode
  case object Quote extends SubscribeMode
  case object Full  extends SubscribeMode
}
