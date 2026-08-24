package com.redmellon.auth

import de.taimos.totp.TOTP
import org.apache.commons.codec.binary.{Base32, Hex}

object TotpGenerator {

  def generate(secret: String): String = {
    val normalized = secret.replaceAll("\\s", "").toUpperCase
    val bytes = new Base32().decode(normalized)
    TOTP.getOTP(Hex.encodeHexString(bytes))
  }
}
