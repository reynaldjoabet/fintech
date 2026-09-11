package finicity.examples

import finicity.api.AuthenticationApi
import finicity.models.PartnerCredentials
import sttp.client4.{DefaultSyncBackend, Request, Response, SyncBackend}

/**
  * An authenticated Finicity session: the app key, a fresh app token, and the backend to send on.
  *
  * Every operation outside of authentication needs *two* headers, but the generated client carries
  * a single `Authorization` value and applies it to both:
  *
  * {{{
  * .auth(authConfig, ApiKeyLocation.HEADER, "Finicity-App-Token")
  * .auth(authConfig, ApiKeyLocation.HEADER, "Finicity-App-Key")
  * }}}
  *
  * Configuring an api with `withApiKeyAuth(appKey)` therefore gets `Finicity-App-Key` right and
  * leaves `Finicity-App-Token` holding the app key too. [[send]] corrects that per request: sttp's
  * `header` replaces an existing header by default.
  */
final case class FinicitySession(appKey: String, appToken: String, backend: SyncBackend) {

  def baseUrl: String = FinicitySession.BaseUrl

  /**
    * Sends a generated request, overwriting the token header with the one from authentication.
    */
  def send[T](request: Request[T]): Response[T] =
    request.withAppToken(appToken).send(backend)

}

object FinicitySession {

  /**
    * Sandbox and production share this host; the app key decides which data you reach.
    */
  val BaseUrl = "https://api.finicity.com"

  private val CredentialVars =
    Seq("FINICITY_APP_KEY", "FINICITY_PARTNER_ID", "FINICITY_PARTNER_SECRET")

  /**
    * Exchanges the partner credentials for an app token and hands `body` a ready session.
    *
    * Environment variables named in `alsoNeeds` are checked up front, so an example that is missing
    * a customer id prints usage instead of authenticating first. With anything missing this makes
    * no network call at all, which keeps every example safe to run anywhere.
    */
  def run(example: String, alsoNeeds: String*)(body: FinicitySession => Unit): Unit = {
    val required = CredentialVars ++ alsoNeeds
    val missing  = required.filterNot(sys.env.contains)

    if (missing.nonEmpty) println(usage(example, required, missing))
    else {
      val appKey  = sys.env("FINICITY_APP_KEY")
      val backend = DefaultSyncBackend()
      try {
        val credentials =
          PartnerCredentials(sys.env("FINICITY_PARTNER_ID"), sys.env("FINICITY_PARTNER_SECRET"))

        // createToken is the one call that takes the app key alone, so it needs no session.
        val token = AuthenticationApi
          .withApiKeyAuth(BaseUrl, appKey)
          .createToken(credentials)
          .send(backend)

        token.body match {
          case Right(accessToken) => body(FinicitySession(appKey, accessToken.token, backend))
          case Left(error)        =>
            println(s"Authentication failed (HTTP ${token.code}): ${error.getMessage}")
        }
      } finally backend.close()
    }
  }

  private def usage(example: String, required: Seq[String], missing: Seq[String]): String = {
    val assignments = required.map(name => s"$name=...").mkString(" ")
    s"""|$example did not run: ${missing.mkString(", ")} not set.
        |
        |  $assignments sbt "examples/runMain finicity.examples.$example"
        |
        |No network call was made.""".stripMargin
  }

}

extension [T](request: Request[T]) {

  /**
    * Replaces the `Finicity-App-Token` header that the generated client filled with the app key.
    */
  def withAppToken(token: String): Request[T] =
    request.header("Finicity-App-Token", token)

}
