package com.redmellon.ingest

import com.redmellon.common.model.Instrument

import java.time.{Instant, LocalDate, ZoneId}

object StockOptionSelector {

  private val ist = ZoneId.of("Asia/Kolkata")

  def expiryDate(i: Instrument): Option[LocalDate] =
    i.expiry.map(ms => Instant.ofEpochMilli(ms).atZone(ist).toLocalDate)

  /** Picks the option contracts of one stock to subscribe.
    *
    * @param options
    *   NFO instruments (any underlying); only CE/PE contracts named `symbol` are considered
    * @param spot
    *   the stock's current price
    * @param today
    *   expiries before this date are ignored, an expiry on this date is still included
    * @param expiryCount
    *   how many upcoming expiries to keep, nearest first
    * @param rangePercent
    *   strikes within spot +/- this percentage are kept
    */
  def select(
    options: Seq[Instrument],
    symbol: String,
    spot: Double,
    today: LocalDate,
    expiryCount: Int,
    rangePercent: Double
  ): Seq[Instrument] = {
    val contracts = options.filter(i => i.name == symbol && (i.instrumentType == "CE" || i.instrumentType == "PE"))
    val expiries = contracts.flatMap(expiryDate).filterNot(_.isBefore(today)).distinct.sorted.take(expiryCount).toSet
    val lower = spot * (1 - rangePercent / 100)
    val upper = spot * (1 + rangePercent / 100)
    contracts.filter { i =>
      expiryDate(i).exists(expiries.contains) && i.strike.toDoubleOption.exists(s => s >= lower && s <= upper)
    }
  }
}
