package finicity.examples

import java.time.temporal.ChronoUnit
import java.time.Instant

import finicity.api.{AccountsApi, TransactionsApi}
import finicity.models.{CustomerAccount, Transaction, Transactions}
import finicity.Authorization

/**
  * Runnable example: list a customer's accounts, then page through one account's transactions.
  *
  * Shows how the generated models handle the parts of the Finicity schema that are easy to get
  * wrong by hand: epoch-second dates typed as `Long`, balances that are absent until the first
  * successful aggregation, and `moreAvailable` typed as `String` on `Transactions` (the spec really
  * does differ from `Customers` and `Institutions`, and the model mirrors it).
  *
  * {{{
  * FINICITY_APP_KEY=... FINICITY_PARTNER_ID=... FINICITY_PARTNER_SECRET=... \
  *   FINICITY_CUSTOMER_ID=... \
  *   sbt "examples/runMain finicity.examples.AccountsAndTransactionsExample"
  * }}}
  */
object AccountsAndTransactionsExample {

  private val PageSize = 25
  private val MaxPages = 3
  private val Window   = 30

  def main(args: Array[String]): Unit =
    FinicitySession.run("AccountsAndTransactionsExample", "FINICITY_CUSTOMER_ID") { session =>
      // run() verified this is set before it authenticated.
      val customerId      = sys.env("FINICITY_CUSTOMER_ID")
      val accountsApi     = AccountsApi.withApiKeyAuth(session.baseUrl, session.appKey)
      val transactionsApi = TransactionsApi.withApiKeyAuth(session.baseUrl, session.appKey)

      session.send(accountsApi.getCustomerAccounts(customerId)).body match {
        case Left(error)     => println(s"Could not list accounts: ${error.getMessage}")
        case Right(accounts) =>
          println(s"${accounts.accounts.size} account(s) for customer $customerId")
          accounts.accounts.foreach(account => println(s"  ${describe(account)}"))

          accounts.accounts.headOption.foreach { account =>
            val toDate   = Instant.now()
            val fromDate = toDate.minus(Window, ChronoUnit.DAYS)
            println(s"\nLast $Window days on ${account.name} (${account.accountNumberDisplay}):")
            page(transactionsApi, session, customerId, account.id, fromDate, toDate, 1, 1)
          }
      }
    }

  private def describe(account: CustomerAccount): String = {
    // `balance` and `balanceDate` are optional: an account exists from the moment it is linked,
    // but carries no balance until an aggregation succeeds.
    val balance =
      account.balance.fold("balance unknown")(amount => f"$amount%,.2f ${account.currency}")
    val asOf = account.balanceDate.fold("")(seconds => s" as of ${Instant.ofEpochSecond(seconds)}")
    s"${account.id} ${account.name} (${account.`type`}, ${account.status}) $balance$asOf"
  }

  private def page(
      api: TransactionsApi[Authorization.ApiKey],
      session: FinicitySession,
      customerId: String,
      accountId: String,
      fromDate: Instant,
      toDate: Instant,
      start: Int,
      pageNumber: Int
  ): Unit = {
    val request = api.getCustomerAccountTransactions(
      customerId = customerId,
      accountId = accountId,
      // fromDate/toDate are required and are epoch *seconds*, which the spec models as Long.
      fromDate = fromDate.getEpochSecond,
      toDate = toDate.getEpochSecond,
      start = Some(start),
      limit = Some(PageSize),
      sort = Some("desc"),
      showDailyBalance = Some(true)
    )

    session.send(request).body match {
      case Left(error)         => println(s"  transactions failed: ${error.getMessage}")
      case Right(transactions) =>
        transactions.transactions.foreach(transaction => println(s"  ${line(transaction)}"))
        summarize(transactions)

        // `start` is 1-based and counts records, not pages: advance it by what came back.
        val more = transactions.moreAvailable.equalsIgnoreCase("true")
        if (more && pageNumber < MaxPages)
          page(
            api,
            session,
            customerId,
            accountId,
            fromDate,
            toDate,
            start + transactions.displaying,
            pageNumber + 1
          )
        else if (more) println(s"  stopping after $MaxPages pages of ${transactions.found}")
    }
  }

  private def line(transaction: Transaction): String = {
    val posted = transaction.postedDate
      .orElse(transaction.transactionDate)
      .fold("pending")(seconds => Instant.ofEpochSecond(seconds).toString)
    // checkNum is Option[Int] since the last regeneration: only checks carry one.
    val check = transaction.checkNum.fold("")(number => s" check#$number")
    f"$posted%-26s ${transaction.amount}%10.2f  ${transaction.description}$check"
  }

  private def summarize(transactions: Transactions): Unit = {
    // Categorization is optional per transaction: pending ones often have none yet.
    val byCategory = transactions.transactions
      .flatMap(_.categorization)
      .groupBy(_.category)
      .view
      .mapValues(_.size)
      .toSeq
      .sortBy { case (_, count) => -count }

    if (byCategory.nonEmpty)
      println(s"  categories: ${byCategory.map((name, count) => s"$name x$count").mkString(", ")}")

    transactions.dailyBalances.foreach { balances =>
      balances.headOption.foreach(balance =>
        println(
          f"  daily balance on ${Instant.ofEpochSecond(balance.date)}: ${balance.ending}%,.2f"
        )
      )
    }
  }

}
