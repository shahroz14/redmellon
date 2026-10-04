package com.redmellon.broker.api

import com.redmellon.common.model.{Exchange, Instrument}

class AngelOneConnect extends AppConnect {
  override def instruments(exchange: Exchange): Seq[Instrument] =
    throw new IllegalArgumentException("Not supported yet")

  override def invalidate(): Unit = ()
}
