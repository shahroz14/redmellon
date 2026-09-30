package com.redmellon.common.model

sealed trait Exchange

object Exchange {
  case object NSE extends Exchange
  case object BSE extends Exchange
  case object NFO extends Exchange
  case object BFO extends Exchange
  case object CDS extends Exchange
  case object MCX extends Exchange
  case object BCD extends Exchange
}
