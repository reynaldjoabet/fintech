package example
//import openbanking.client.model
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.{
  CodecMakerConfig,
  JsonCodecMaker
}

import finicity.models.*
import finicity.api.IdentityApi
import finicity.api.AnalyticsApi
import finicity.api.AccountsApi
import finicity.api.AppRegistrationApi
import finicity.api.AuthenticationApi
import finicity.api.VerifyAssetsApi
import finicity.api.BankStatementsApi
import finicity.api.CashFlowApi
import finicity.api.ConnectApi
import finicity.api.ConsumersApi
import finicity.api.CustomersApi
import finicity.api.PortfoliosApi
import finicity.api.ReportsApi
import finicity.api.InstitutionsApi
import finicity.api.PayStatementsApi
import finicity.api.PaymentsApi
import finicity.api.TransactionsApi
import finicity.api.TxPushApi
import finicity.api.VerifyIncomeAndEmploymentApi
object Main extends App with Greeting {
  println(greeting)

  val account = CustomerAccount(
    id = "123",
    number = "456",
    accountNumberDisplay = "456",
    name = "My Account",
    balance = Some(1000.0),
    `type` = "Checking",
    status = "Active",
    customerId = "789",
    institutionId = "101112",
    createdDate = 1625097600L,
    currency = "USD",
    institutionLoginId = 131415,
    displayPosition = Some(1)
  )

}
