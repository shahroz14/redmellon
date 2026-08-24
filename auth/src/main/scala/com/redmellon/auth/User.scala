package com.redmellon.auth

case class User(
  username: String,
  fullName: String,
  passwordHash: String,
  account: Option[AccountCredentials] = None
)
