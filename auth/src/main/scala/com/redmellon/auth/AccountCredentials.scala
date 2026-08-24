package com.redmellon.auth

import scala.beans.BeanProperty

class AccountCredentials {
  @BeanProperty var userId: String = _
  @BeanProperty var password: String = _
  @BeanProperty var totpSecret: String = _
  @BeanProperty var apps: java.util.List[AppCredentials] = new java.util.ArrayList()
}
