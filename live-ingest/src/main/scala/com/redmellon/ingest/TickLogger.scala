package com.redmellon.ingest

import com.redmellon.common.disk.QueueBackedCsvAppender
import com.redmellon.common.model.Tick
import com.redmellon.common.util.Logging

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.collection.mutable

class TickLogger extends Logging {

  private val basePath = s"${System.getProperty("user.home")}/redmellon/ticks"
  private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  private val appenders = mutable.Map.empty[Long, QueueBackedCsvAppender[Tick]]
  private val lastVolumeMap = mutable.Map.empty[Long, Long]

  def log(tick: Tick): Unit = {
    val appender = appenders.getOrElseUpdate(
      tick.instrumentToken, {
        val date = LocalDate.now().format(dateFmt)
        val filePath = s"$basePath/$date/${tick.symbol}_${tick.exchangeToken}.csv"
        new QueueBackedCsvAppender[Tick](filePath, Tick.TickCsvConverter)
      }
    )
    val lastVolume = lastVolumeMap.getOrElse(tick.instrumentToken, -1L)
    if (tick.volume != lastVolume) {
      appender.add(Tick.TickCsvConverter.toCsvRow(tick))
      lastVolumeMap.update(tick.instrumentToken, tick.volume)
    }
  }
}
