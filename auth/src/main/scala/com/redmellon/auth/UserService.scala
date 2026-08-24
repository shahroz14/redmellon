package com.redmellon.auth

import com.redmellon.common.util.Logging
import org.mindrot.jbcrypt.BCrypt

import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters._

class UserService extends Logging {

  private val users = new ConcurrentHashMap[String, User]()

  def createUser(username: String, fullName: String, password: Array[Char]): User = {
    val hash = BCrypt.hashpw(new String(password), BCrypt.gensalt())
    val user = User(username = username, fullName = fullName, passwordHash = hash)
    val existing = users.putIfAbsent(username, user)
    if (existing != null) throw new IllegalArgumentException(s"User '$username' already exists")
    info(s"Created user: $username")
    user
  }

  def checkPassword(username: String, password: Array[Char]): Boolean =
    getUser(username).exists(u => BCrypt.checkpw(new String(password), u.passwordHash))

  def setAccount(username: String, account: AccountCredentials): Unit = {
    users.compute(
      username,
      (_, current) => {
        require(current != null, s"User '$username' not found")
        current.copy(account = Some(account))
      }
    )
    info(s"Set account ${account.userId} for user $username")
  }

  def getUser(username: String): Option[User] = Option(users.get(username))
  def getAllUsers: Map[String, User]          = users.asScala.toMap
}
