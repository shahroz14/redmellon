package com.redmellon.auth

import scala.beans.BeanProperty

class AppCredentials {
  @BeanProperty var apiKey: String = _
  @BeanProperty var apiSecret: String = _
  @BeanProperty var appName: String = _
}
