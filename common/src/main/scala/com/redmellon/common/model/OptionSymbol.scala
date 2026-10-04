package com.redmellon.common.model

import java.time.format.TextStyle
import java.time.{Instant, LocalDate, Month, ZoneId}
import java.util.Locale

case class OptionSymbol(
  index: String,
  expiry: LocalDate,
  strike: Double,
  optionType: String // "CE" or "PE"
) {
  def toNormalizedName: String = {
    val dd = f"${expiry.getDayOfMonth}%02d"
    val mmm = expiry.getMonth.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase
    val yy = expiry.getYear.toString.takeRight(2)
    val strikeStr = if (strike == strike.toLong.toDouble) strike.toLong.toString else strike.toString
    s"$index$dd$mmm$yy$strikeStr$optionType"
  }
}

object OptionSymbol {
  private val ist = ZoneId.of("Asia/Kolkata")

  // Zerodha single-char month codes used in weekly expiry symbols
  private val weeklyMonthCodes: Map[Char, Month] = Map(
    '1' -> Month.JANUARY,
    '2' -> Month.FEBRUARY,
    '3' -> Month.MARCH,
    '4' -> Month.APRIL,
    '5' -> Month.MAY,
    '6' -> Month.JUNE,
    '7' -> Month.JULY,
    '8' -> Month.AUGUST,
    '9' -> Month.SEPTEMBER,
    'O' -> Month.OCTOBER,
    'N' -> Month.NOVEMBER,
    'D' -> Month.DECEMBER
  )

  private val monthlyMonthNames: Map[String, Month] = Map(
    "JAN" -> Month.JANUARY,
    "FEB" -> Month.FEBRUARY,
    "MAR" -> Month.MARCH,
    "APR" -> Month.APRIL,
    "MAY" -> Month.MAY,
    "JUN" -> Month.JUNE,
    "JUL" -> Month.JULY,
    "AUG" -> Month.AUGUST,
    "SEP" -> Month.SEPTEMBER,
    "OCT" -> Month.OCTOBER,
    "NOV" -> Month.NOVEMBER,
    "DEC" -> Month.DECEMBER
  )

  def parse(symbol: String, expiryMillis: Option[Long]): Option[OptionSymbol] = {
    val optionType = symbol.takeRight(2)
    if (optionType != "CE" && optionType != "PE") return None

    val body = symbol.dropRight(2)         // e.g. "NIFTY26N0323750"
    val index = body.takeWhile(_.isLetter) // e.g. "NIFTY"
    val rest = body.drop(index.length)     // e.g. "26N0323750"

    if (index.isEmpty || rest.length < 3) return None

    val yy = rest.take(2)
    if (!yy.forall(_.isDigit)) return None
    val year = 2000 + yy.toInt
    val afterYear = rest.drop(2) // e.g. "N0323750" or "NOV21600"

    if (afterYear.length < 2) return None

    // weekly: afterYear[1] is a digit (single-char month code + 2-digit day)
    // monthly: afterYear[1] is a letter (3-char month abbreviation)
    if (afterYear(1).isDigit) parseWeekly(index, year, afterYear, optionType)
    else parseMonthly(index, year, afterYear, optionType, expiryMillis)
  }

  private def parseWeekly(index: String, year: Int, afterYear: String, optionType: String): Option[OptionSymbol] =
    for {
      month <- weeklyMonthCodes.get(afterYear(0))
      dd = afterYear.substring(1, 3)
      if dd.forall(_.isDigit)
      strike <- afterYear.drop(3).toDoubleOption
    } yield OptionSymbol(index, LocalDate.of(year, month, dd.toInt), strike, optionType)

  private def parseMonthly(
    index: String,
    year: Int,
    afterYear: String,
    optionType: String,
    expiryMillis: Option[Long]
  ): Option[OptionSymbol] =
    for {
      month      <- monthlyMonthNames.get(afterYear.take(3).toUpperCase)
      strike     <- afterYear.drop(3).toDoubleOption
      expiryDate <- expiryMillis.map(ms => Instant.ofEpochMilli(ms).atZone(ist).toLocalDate)
    } yield OptionSymbol(index, expiryDate, strike, optionType)
}
