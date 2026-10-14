package com.redmellon.broker.api

import com.redmellon.common.model.{SubscribeMode, Tick}

trait AppTicker {
  def subscribe(tokens: Seq[Long], mode: SubscribeMode, onTick: Seq[Tick] => Unit): Unit

  def disconnect(): Unit = ()
}
