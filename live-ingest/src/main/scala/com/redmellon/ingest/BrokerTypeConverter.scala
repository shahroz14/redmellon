package com.redmellon.ingest

import com.redmellon.common.BrokerName
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
@ConfigurationPropertiesBinding
class BrokerTypeConverter extends Converter[String, BrokerName] {
  override def convert(source: String): BrokerName = source.toUpperCase match {
    case "ZERODHA"  => BrokerName.Zerodha
    case "ANGELONE" => BrokerName.AngelOne
    case other      => throw new IllegalArgumentException(s"Unknown broker type: $other")
  }
}
