package finicity.examples

import com.github.plokhotnyuk.jsoniter_scala.core.{readFromString, writeToString}
import finicity.api.InstitutionsApi
import finicity.models.{Categorization, Institutions, Transaction}
import finicity.JsonSupport.given
import sttp.client4.testing.SyncBackendStub
import sttp.client4.ResponseException
import sttp.model.StatusCode

/**
  * Offline example: exercise the generated client and its models without credentials or network.
  *
  * Every operation returns an sttp `Request` value, so it can be inspected before it is sent and
  * run against `SyncBackendStub` instead of a real backend -- which is how you unit-test code built
  * on the generated client. This example also shows all three outcomes the response type
  * `Either[ResponseException[String], A]` encodes, and uses the derived codecs directly.
  *
  * Needs no environment variables:
  *
  * {{{
  * sbt "examples/runMain finicity.examples.StubbedBackendExample"
  * }}}
  */
object StubbedBackendExample {

  private val InstitutionsJson =
    """{
      |  "found": 2,
      |  "displaying": 2,
      |  "moreAvailable": false,
      |  "createdDate": 1735689600,
      |  "institutions": [
      |    {
      |      "id": 102105, "name": "FinBank Profiles A", "transAgg": true, "ach": true,
      |      "stateAgg": false, "voi": true, "voa": true, "aha": false, "availBalance": true,
      |      "accountOwner": true, "oauthEnabled": false, "currency": "USD", "status": "online"
      |    },
      |    {
      |      "id": 102176, "name": "FinBank OAuth", "transAgg": true, "ach": false,
      |      "stateAgg": false, "voi": false, "voa": true, "aha": false, "availBalance": true,
      |      "accountOwner": false, "oauthEnabled": true, "currency": "USD", "status": "online"
      |    }
      |  ]
      |}""".stripMargin

  private val NotFoundJson =
    """{"code": 14001, "message": "Institution not found", "status": "404"}"""

  private val TruncatedJson = """{"found": 2, "displaying":"""

  def main(args: Array[String]): Unit = {
    val backend = SyncBackendStub
      .whenRequestMatches(_.uri.path.lastOption.contains("77777777"))
      .thenRespondAdjust(NotFoundJson, StatusCode.NotFound)
      .whenRequestMatches(_.uri.path.lastOption.contains("88888888"))
      .thenRespondAdjust(TruncatedJson)
      .whenRequestMatches(_.uri.path.lastOption.contains("institutions"))
      .thenRespondAdjust(InstitutionsJson)

    // A fake key is enough: the stub never checks headers, and the request is only a value until
    // it is sent.
    val api     = InstitutionsApi.withApiKeyAuth("https://api.finicity.com", "demo-app-key")
    val request = api.getInstitutions(search = Some("FinBank"), limit = Some(5))

    println(s"request: ${request.method} ${request.uri}")
    // Header names only -- the values are credentials, and credentials do not belong in logs.
    println(s"headers: ${request.headers.map(_.name).mkString(", ")}")

    println("\n-- 200: decoded into the generated model")
    request.send(backend).body match {
      case Right(institutions) =>
        println(s"found ${institutions.found}, displaying ${institutions.displaying}")
        institutions.institutions.foreach(institution =>
          println(s"  ${institution.id} ${institution.name.getOrElse("<unnamed>")}")
        )
      case Left(error) => println(s"unexpected: ${error.getMessage}")
    }

    println("\n-- 404: the error body stays a String")
    api.getInstitution(77777777L).send(backend).body match {
      // The generator derives codecs only for success schemas, so an error body arrives raw.
      // Decode it yourself (with a JsonValueCodec[ErrorMessage]) if you need it as a model.
      case Left(ResponseException.UnexpectedStatusCode(body, meta)) =>
        println(s"HTTP ${meta.code}: $body")
      case Left(error)    => println(s"unexpected: $error")
      case Right(wrapper) =>
        println(s"unexpected success: ${wrapper.institution.id}")
    }

    println("\n-- 200 with a truncated body: a deserialization failure, not a status failure")
    api.getInstitution(88888888L).send(backend).body match {
      case Left(ResponseException.DeserializationException(body, cause, _)) =>
        println(s"could not decode '$body': ${cause.getMessage.linesIterator.next()}")
      case Left(error) =>
        println(s"unexpected: $error")
      case Right(wrapper) =>
        println(s"unexpected success: ${wrapper.institution.id}")
    }

    codecRoundTrip()
    backend.close()
  }

  /**
    * The codecs the client sends and receives with are plain jsoniter givens; use them directly.
    */
  private def codecRoundTrip(): Unit = {
    println("\n-- codecs without a request")

    val institutions = readFromString[Institutions](InstitutionsJson)
    println(s"parsed ${institutions.institutions.size} institutions from a String")

    val transaction = Transaction(
      id = 12345678L,
      amount = -42.5d,
      accountId = 5011234567L,
      customerId = 1005061234L,
      status = "active",
      description = "COFFEE ROASTERS",
      createdDate = 1735689600L,
      checkNum = Some(1042),
      categorization = Some(
        Categorization(
          normalizedPayeeName = "Coffee Roasters",
          category = "Restaurants",
          country = "USA"
        )
      )
    )

    // None fields are omitted: jsoniter's default `transientNone` keeps the payload to what was
    // actually set, which is what Finicity expects on write paths.
    println(writeToString(transaction))
  }

}
