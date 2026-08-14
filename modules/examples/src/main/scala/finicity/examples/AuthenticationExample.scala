package finicity.examples

import finicity.api.AuthenticationApi
import finicity.models.{AccessToken, PartnerCredentials}
import sttp.client4.{DefaultSyncBackend, Response, ResponseException}

/**
  * Runnable example: exchange partner credentials for a Finicity access token.
  *
  * The generated client returns each call as an sttp `Request` value -- nothing is sent until you
  * run it against a backend. Here we use `DefaultSyncBackend` (JDK HttpClient, from sttp-core), so
  * this needs no extra backend dependency.
  *
  * Credentials come from the environment so nothing secret is committed:
  *
  * {{{
  * FINICITY_APP_KEY=...        \
  * FINICITY_PARTNER_ID=...     \
  * FINICITY_PARTNER_SECRET=... \
  *   sbt "examples/runMain finicity.examples.AuthenticationExample"
  * }}}
  *
  * With no env vars set it prints usage and exits without a network call, so it stays safe to run
  * in any environment.
  */
object AuthenticationExample {

  def main(args: Array[String]): Unit = {
    val creds =
      for {
        appKey        <- sys.env.get("FINICITY_APP_KEY")
        partnerId     <- sys.env.get("FINICITY_PARTNER_ID")
        partnerSecret <- sys.env.get("FINICITY_PARTNER_SECRET")
      } yield (appKey, PartnerCredentials(partnerId, partnerSecret))

    creds match {
      case None =>
        println(
          """Set FINICITY_APP_KEY, FINICITY_PARTNER_ID and FINICITY_PARTNER_SECRET
            |to call the live sandbox. Skipping the network call.""".stripMargin
        )
      case Some((appKey, partnerCredentials)) =>
        // The Finicity-App-Key header carries the app key; createToken requires
        // the client to be configured with ApiKey auth, enforced at compile time
        // by the `Auth <:< Authorization.ApiKey` bound on the method.
        val api = AuthenticationApi.withApiKeyAuth(
          baseUrl = "https://api.finicity.com",
          apiKey = appKey
        )

        val backend = DefaultSyncBackend()
        try {
          val response: Response[Either[ResponseException[String], AccessToken]] =
            api.createToken(partnerCredentials).send(backend)

          response.body match {
            case Right(AccessToken(token)) =>
              // A real client would cache this and refresh it after ~90 minutes;
              // it goes in the Finicity-App-Token header on subsequent calls.
              println(
                s"Got access token (length ${token.length}), expires in ~2h"
              )
            case Left(error) =>
              println(
                s"Authentication failed (HTTP ${response.code}): ${error.getMessage}"
              )
          }
        } finally backend.close()
    }
  }

}
