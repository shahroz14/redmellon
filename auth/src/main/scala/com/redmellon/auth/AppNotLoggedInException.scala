package com.redmellon.auth

class AppNotLoggedInException(appName: String)
    extends RuntimeException(s"App '$appName' is not logged in — call login() first")
