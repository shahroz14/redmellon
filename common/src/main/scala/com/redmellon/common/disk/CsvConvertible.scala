package com.redmellon.common.disk

trait CsvConvertible[T] {
  def toCsvRow(obj: T): String
  def toHeader: String
  def fromCsv(values: String*): T
}
