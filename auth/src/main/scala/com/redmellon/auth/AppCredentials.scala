package com.redmellon.auth

import scala.beans.BeanProperty

class AppCredentials {
  @BeanProperty var apiKey: String = _
  @BeanProperty var apiSecret: String = _
}
