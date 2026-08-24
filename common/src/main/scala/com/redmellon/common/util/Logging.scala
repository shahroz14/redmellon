package com.redmellon.common.util

import org.slf4j.{Logger, LoggerFactory}

trait Logging {

  @transient private var _logger = null.asInstanceOf[Logger]

  def logger: Logger = {
    if (_logger == null) {
      _logger = LoggerFactory.getLogger(getClass)
    }
    _logger
  }

  def info(msg: String): Unit = logger.info(msg)

  def debug(msg: String): Unit = logger.debug(msg)

  def warn(msg: String, t: Throwable): Unit = logger.warn(msg, t)

  def warn(msg: String): Unit = logger.warn(msg)

  def error(msg: String): Unit = logger.error(msg)

  def error(msg: String, t: Throwable): Unit = logger.error(msg, t)

}
