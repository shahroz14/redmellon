package com.redmellon.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import java.net.{URI, URLDecoder, URLEncoder}
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.{HttpClient, HttpRequest}
import java.nio.charset.StandardCharsets
import scala.jdk.CollectionConverters._

object ZerodhaLoginUtils {

  private val http = HttpClient.newHttpClient()
  private val noRedirectHttp = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()
  private val mapper = new ObjectMapper().registerModule(DefaultScalaModule)

  def getRequestToken(credentials: AccountCredentials, apiKey: String): String = {
    // Step 1: submit credentials
    val loginResp = http.send(
      HttpRequest.newBuilder()
        .uri(URI.create("https://kite.zerodha.com/api/login"))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .header("X-Kite-Version", "3")
        .POST(BodyPublishers.ofString(s"user_id=${enc(credentials.userId)}&password=${enc(credentials.password)}"))
        .build(),
      BodyHandlers.ofString()
    )
    if (loginResp.statusCode() != 200)
      throw new RuntimeException(s"Login failed: HTTP ${loginResp.statusCode()} — ${loginResp.body()}")

    val loginData = mapper.readTree(loginResp.body()).path("data")
    val requestId = loginData.path("request_id").asText()
    val loginCookies = parseCookies(loginResp.headers().allValues("set-cookie").asScala.toList)

    // Step 2: submit TOTP
    val twoFaResp = noRedirectHttp.send(
      HttpRequest.newBuilder()
        .uri(URI.create("https://kite.zerodha.com/api/twofa"))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .header("X-Kite-Version", "3")
        .header("Cookie", cookieHeader(loginCookies))
        .POST(BodyPublishers.ofString(
          s"user_id=${enc(credentials.userId)}&request_id=$requestId" +
            s"&twofa_value=${TotpGenerator.generate(credentials.totpSecret)}&twofa_type=totp&skip_session=True"
        ))
        .build(),
      BodyHandlers.ofString()
    )
    if (twoFaResp.statusCode() != 200)
      throw new RuntimeException(s"2FA failed: HTTP ${twoFaResp.statusCode()} — ${twoFaResp.body()}")

    val allCookies = loginCookies ++ parseCookies(twoFaResp.headers().allValues("set-cookie").asScala.toList)

    // Step 3: connect/login → connect/finish (intermediate redirect)
    val connectResp = noRedirectHttp.send(
      HttpRequest.newBuilder()
        .uri(URI.create(s"https://kite.zerodha.com/connect/login?v=3&api_key=$apiKey"))
        .header("Cookie", cookieHeader(allCookies))
        .GET()
        .build(),
      BodyHandlers.ofString()
    )
    if (connectResp.statusCode() != 302 && connectResp.statusCode() != 303)
      throw new RuntimeException(s"Connect redirect failed: HTTP ${connectResp.statusCode()} — ${connectResp.body()}")

    val finishUrl = connectResp.headers().firstValue("Location")
      .orElseThrow(() => new RuntimeException("No Location header in connect redirect"))

    // Step 4: connect/finish → callback URL with request_token
    val finishResp = noRedirectHttp.send(
      HttpRequest.newBuilder()
        .uri(URI.create(finishUrl))
        .header("Cookie", cookieHeader(allCookies))
        .GET()
        .build(),
      BodyHandlers.ofString()
    )

    val callbackUrl = finishResp.headers().firstValue("Location")
      .orElseThrow(() =>
        new RuntimeException(s"No Location header from connect/finish (HTTP ${finishResp.statusCode()})")
      )

    extractQueryParam(callbackUrl, "request_token")
  }

  private def parseCookies(setCookieHeaders: List[String]): Map[String, String] =
    setCookieHeaders.flatMap { h =>
      h.split(";").head.trim.split("=", 2) match {
        case Array(k, v) => Some(k.trim -> v.trim)
        case _           => None
      }
    }.toMap

  private def cookieHeader(cookies: Map[String, String]): String =
    cookies.map { case (k, v) => s"$k=$v" }.mkString("; ")

  private def enc(s: String): String = URLEncoder.encode(s, StandardCharsets.UTF_8)

  private def extractQueryParam(url: String, key: String): String = {
    val value = Option(URI.create(url).getQuery)
      .toSeq
      .flatMap(_.split("&"))
      .map(_.split("=", 2))
      .collectFirst { case Array(k, v) if k == key => v }
      .getOrElse(throw new RuntimeException(s"'$key' not found in redirect URL: $url"))
    URLDecoder.decode(value, StandardCharsets.UTF_8)
  }
}
