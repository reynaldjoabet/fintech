package finicity.examples

import finicity.api.InstitutionsApi
import finicity.models.Institution
import finicity.Authorization

/**
  * Runnable example: search the institution directory, then fetch one institution in full.
  *
  * Institutions need no customer, so this is the cheapest end-to-end check that credentials work.
  * It also shows two generator conventions worth knowing: reserved words are escaped with backticks
  * (`type`), and a single-resource response can arrive wrapped (`InstitutionWrapper(institution)`)
  * while the list response is not.
  *
  * {{{
  * FINICITY_APP_KEY=... FINICITY_PARTNER_ID=... FINICITY_PARTNER_SECRET=... \
  *   [FINICITY_INSTITUTION_SEARCH=Chase] \
  *   sbt "examples/runMain finicity.examples.InstitutionSearchExample"
  * }}}
  */
object InstitutionSearchExample {

  def main(args: Array[String]): Unit =
    FinicitySession.run("InstitutionSearchExample") { session =>
      val api    = InstitutionsApi.withApiKeyAuth(session.baseUrl, session.appKey)
      val search = sys.env.getOrElse("FINICITY_INSTITUTION_SEARCH", "FinBank")

      // `type` is a Scala keyword, so the generated parameter is escaped and passed in backticks.
      val request =
        api.getInstitutions(search = Some(search), limit = Some(5), `type` = Some("voa"))

      session.send(request).body match {
        case Left(error)   => println(s"Institution search failed: ${error.getMessage}")
        case Right(result) =>
          println(s"${result.found} institution(s) match '$search' with VOA support")
          result.institutions.foreach(institution => println(s"  ${summary(institution)}"))
          if (result.moreAvailable) println("  more available: page with start/limit")
          result.institutions.headOption.foreach(institution =>
            detail(api, session, institution.id)
          )
      }
    }

  private def summary(institution: Institution): String = {
    // Product support arrives as a row of required booleans rather than a list, so collect the
    // ones that are on.
    val products = Seq(
      "transAgg"     -> institution.transAgg,
      "ach"          -> institution.ach,
      "voa"          -> institution.voa,
      "voi"          -> institution.voi,
      "accountOwner" -> institution.accountOwner,
      "availBalance" -> institution.availBalance
    ).collect { case (product, true) => product }

    // `name` and `displayName` are both optional even though every real institution has one.
    val name  = institution.displayName.orElse(institution.name).getOrElse("<unnamed>")
    val oauth = if (institution.oauthEnabled) " oauth" else ""
    s"${institution.id} $name [${institution.status}]$oauth ${products.mkString(",")}"
  }

  private def detail(
      api: InstitutionsApi[Authorization.ApiKey],
      session: FinicitySession,
      institutionId: Long
  ): Unit =
    session.send(api.getInstitution(institutionId)).body match {
      case Left(error)    => println(s"  detail failed: ${error.getMessage}")
      case Right(wrapper) =>
        val institution = wrapper.institution
        println(s"\n${institution.id}: ${institution.displayName.getOrElse("<unnamed>")}")
        institution.address.foreach { address =>
          val lines = Seq(address.addressLine1, address.city, address.state, address.postalCode)
          println(s"  address: ${lines.flatten.mkString(", ")}")
        }
        institution.branding.flatMap(_.logo).foreach(logo => println(s"  logo: $logo"))
        institution.specialInstructions.foreach(instructions =>
          instructions.foreach(instruction => println(s"  note: $instruction"))
        )
    }

}
