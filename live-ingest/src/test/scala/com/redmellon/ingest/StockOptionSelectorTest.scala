package com.redmellon.ingest

import com.redmellon.common.BrokerName
import com.redmellon.common.model.Instrument
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.{LocalDate, ZoneId}

class StockOptionSelectorTest extends AnyFlatSpec with Matchers {

  private val ist = ZoneId.of("Asia/Kolkata")
  private def millis(d: LocalDate) = d.atStartOfDay(ist).toInstant.toEpochMilli

  private def opt(name: String, strike: Double, expiry: LocalDate, kind: String = "CE", token: Long = 0): Instrument =
    Instrument(
      token,
      token,
      s"$name-$strike-$kind",
      name,
      "NFO",
      "NFO-OPT",
      kind,
      0,
      0.05,
      500,
      strike.toString,
      Some(millis(expiry)),
      BrokerName.Zerodha
    )

  private val today = LocalDate.of(2026, 11, 8)
  private val e1 = LocalDate.of(2026, 11, 24)
  private val e2 = LocalDate.of(2026, 12, 29)
  private val e3 = LocalDate.of(2027, 1, 26)

  private def select(options: Seq[Instrument], spot: Double = 1000) =
    StockOptionSelector.select(options, "RELIANCE", spot, today, expiryCount = 2, rangePercent = 20)

  "select" should "keep only the next two expiries" in {
    val options = Seq(e1, e2, e3).map(opt("RELIANCE", 1000, _))
    select(options).flatMap(StockOptionSelector.expiryDate).toSet shouldBe Set(e1, e2)
  }

  it should "keep strikes within +/- 20% of spot, inclusive of the bounds" in {
    val options = Seq(799.0, 800.0, 1000.0, 1200.0, 1201.0).map(opt("RELIANCE", _, e1))
    select(options).map(_.strike.toDouble) shouldBe Seq(800.0, 1000.0, 1200.0)
  }

  it should "keep both CE and PE" in {
    val options = Seq(opt("RELIANCE", 1000, e1, "CE"), opt("RELIANCE", 1000, e1, "PE"))
    select(options).map(_.instrumentType).toSet shouldBe Set("CE", "PE")
  }

  it should "include an expiry that falls on today and skip past ones" in {
    val options = Seq(today, today.minusDays(3), e1, e2).map(opt("RELIANCE", 1000, _))
    select(options).flatMap(StockOptionSelector.expiryDate).toSet shouldBe Set(today, e1)
  }

  it should "ignore other stocks and non-option contracts" in {
    val future = opt("RELIANCE", 0, e1, "FUT")
    val other = opt("RELIANCEPOWER", 1000, e1)
    val options = Seq(future, other, opt("RELIANCE", 1000, e1))
    select(options).map(_.name) shouldBe Seq("RELIANCE")
    select(options).map(_.instrumentType) shouldBe Seq("CE")
  }

  it should "pick expiries from the stock's own contracts, not from other stocks" in {
    val options =
      Seq(opt("TCS", 1000, LocalDate.of(2026, 11, 10)), opt("RELIANCE", 1000, e1), opt("RELIANCE", 1000, e2))
    select(options).flatMap(StockOptionSelector.expiryDate).toSet shouldBe Set(e1, e2)
  }

  it should "return nothing when the stock has no options" in {
    select(Seq(opt("TCS", 1000, e1))) shouldBe empty
  }
}
