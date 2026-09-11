package finicity.examples

import java.time.Instant

import finicity.api.CustomersApi
import finicity.models.{CreatedCustomer, CustomerUpdate, Customers, NewCustomer}
import finicity.Authorization

/**
  * Runnable example: enroll a testing customer, read it back, rename it, then delete it.
  *
  * Shows the shapes the generator produces for a whole CRUD group:
  *   - request models are plain case classes with `Option` defaults (`NewCustomer`)
  *   - 201/200 bodies decode into models (`CreatedCustomer`, `Customers`)
  *   - 204 bodies decode into `Unit`, so success is `Right(())`
  *   - an inline response schema gets named after its operation (`GetCustomer200Response`)
  *
  * {{{
  * FINICITY_APP_KEY=... FINICITY_PARTNER_ID=... FINICITY_PARTNER_SECRET=... \
  *   sbt "examples/runMain finicity.examples.CustomerLifecycleExample"
  * }}}
  */
object CustomerLifecycleExample {

  private type Api = CustomersApi[Authorization.ApiKey]

  def main(args: Array[String]): Unit =
    FinicitySession.run("CustomerLifecycleExample") { session =>
      val api = CustomersApi.withApiKeyAuth(session.baseUrl, session.appKey)

      // Only `username` is required; the rest of NewCustomer defaults to None and, because
      // jsoniter omits None by default, never reaches the wire.
      val newCustomer = NewCustomer(
        username = s"example-user-${Instant.now().getEpochSecond}",
        firstName = Some("Ada"),
        lastName = Some("Lovelace")
      )

      // addTestingCustomer, not addCustomer: testing customers are not billable and can only
      // reach the FinBank test profiles.
      session.send(api.addTestingCustomer(newCustomer)).body match {
        case Left(error) =>
          println(s"Could not enroll ${newCustomer.username}: ${error.getMessage}")
        case Right(created) =>
          val enrolledAt = Instant.ofEpochSecond(created.createdDate)
          println(s"Enrolled ${created.username} as ${created.id} at $enrolledAt")
          try {
            rename(api, session, created)
            show(api, session, created)
            findByUsername(api, session, created.username)
          } finally delete(api, session, created)
      }
    }

  /**
    * 204 No Content: the operation returns `Request[Either[ResponseException[String], Unit]]`.
    */
  private def rename(api: Api, session: FinicitySession, customer: CreatedCustomer): Unit = {
    val update = CustomerUpdate(lastName = Some("Byron"))

    session.send(api.modifyCustomer(customer.id, update)).body match {
      case Right(_)    => println(s"  renamed to ${update.lastName.getOrElse("")}")
      case Left(error) => println(s"  rename failed: ${error.getMessage}")
    }
  }

  private def show(api: Api, session: FinicitySession, customer: CreatedCustomer): Unit =
    session.send(api.getCustomer(customer.id)).body match {
      // The 200 body was an inline schema in the spec, so the generator named it after the
      // operation: GetCustomer200Response is a Customer plus applicationId.
      case Right(fetched) =>
        val name = Seq(fetched.firstName, fetched.lastName).flatten.mkString(" ")
        println(s"  fetched: type=${fetched.`type`} name=${if (name.isEmpty) "<unset>" else name}")
      case Left(error) => println(s"  fetch failed: ${error.getMessage}")
    }

  private def findByUsername(api: Api, session: FinicitySession, username: String): Unit = {
    // Optional query params are Option-typed and dropped from the URL when None, so passing
    // only `username` and `limit` sends exactly two query parameters.
    val request = api.getCustomers(username = Some(username), limit = Some(5))

    session.send(request).body match {
      case Right(Customers(displaying, moreAvailable, customers, found)) =>
        println(s"  search: displaying $displaying of ${found.getOrElse(displaying)}")
        customers.foreach(customer =>
          println(s"    ${customer.id} ${customer.username} (${customer.`type`})")
        )
        if (moreAvailable) println("    more available: raise `start` by `displaying` to page on")
      case Left(error) =>
        println(s"  search failed: ${error.getMessage}")
    }
  }

  private def delete(api: Api, session: FinicitySession, customer: CreatedCustomer): Unit =
    session.send(api.deleteCustomer(customer.id)).body match {
      case Right(_)    => println(s"Deleted ${customer.id}")
      case Left(error) => println(s"Could not delete ${customer.id}: ${error.getMessage}")
    }

}
