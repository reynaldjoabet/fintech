package example
//import openbanking.client.model
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.{
  CodecMakerConfig,
  JsonCodecMaker
}

import finicity.models.*
import finicity.api.AccountOwnerApi
import finicity.api.AnalyticsAndAttributesApi
import finicity.api.AccountsApi
import finicity.api.AppRegistrationAndOAuthMigrationApi
import finicity.api.AuthenticationApi
import finicity.api.AssetApi
import finicity.api.BankStatementsApi
import finicity.api.CashFlowApi
import finicity.api.ConnectApi
import finicity.api.ConsumerApi
import finicity.api.CustomerApi
import finicity.api.DeprecatedApi
import finicity.api.GetPortfoliosApi
import finicity.api.GetReportsByConsumerApi
import finicity.api.GetReportsByCustomerApi
import finicity.api.InstitutionsApi
import finicity.api.LiabilitiesApi
import finicity.api.PayStatementsApi
import finicity.api.PaymentsApi
import finicity.api.TransactionsApi
import finicity.api.TxPushApi
import finicity.api.VerifyAssetsApi
import finicity.api.VerifyEmploymentApi
import finicity.api.VerifyIncomeAndEmploymentApi
import finicity.api.VerifyIncomeApi
object Main extends Greeting with App {
  println(greeting)

  val account = Account(
    id = "123",
    number = "456",
    name = "My Account",
    balance = 1000.0,
    `type` = "Checking",
    status = "Active",
    customerId = "789",
    institutionId = "101112",
    createdDate = 1625097600,
    currency = "USD",
    institutionLoginId = 131415,
    displayPosition = 1
  )

}
