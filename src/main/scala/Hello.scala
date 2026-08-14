package example

import finicity.models.*
object Hello extends App with Greeting {

  println(greeting)

  val checkingAccount = CustomerAccount(
    id = "123",
    number = "456",
    accountNumberDisplay = "456",
    name = "My Account",
    balance = Some(1000.0),
    `type` = "Checking",
    status = "Active",
    customerId = "789",
    institutionId = "101112",
    createdDate = 1622548800L,
    currency = "USD",
    institutionLoginId = 131415,
    displayPosition = Some(1)
  )

  val savingsAccount = CustomerAccount(
    id = "124",
    number = "457",
    accountNumberDisplay = "457",
    name = "My Savings Account",
    balance = Some(5000.0),
    `type` = "Savings",
    status = "Active",
    customerId = "789",
    institutionId = "101112",
    createdDate = 1622548800L,
    currency = "USD",
    institutionLoginId = 131415,
    displayPosition = Some(2)
  )

  val accountDetail = AccountDetails(
    interestMarginBalance = Some(0.0),
    availableCashBalance = Some(1000.0),
    vestedBalance = Some(1000.0),
    currentLoanBalance = Some(0.0),
    availableBalanceAmount = Some(1000.0)
  )

  val transactions = List(
    Transaction(
      id = 8L,
      amount = -50.0,
      accountId = 123L,
      customerId = 789L,
      status = "Active",
      description = "Grocery Store",
      createdDate = 1622548800L,
      memo = Some(""),
      `type` = Some("debit"),
      interestAmount = Some(0.0),
      principalAmount = Some(0.0),
      feeAmount = Some(0.0),
      escrowAmount = Some(0.0),
      unitQuantity = Some(0.0),
      postedDate = Some(1622707200L),
      transactionDate = Some(1622707200L),
      categorization = Some(
        Categorization(
          normalizedPayeeName = "Grocery Store",
          category = "Food",
          country = "USA",
          city = Some("New York"),
          state = Some("NY"),
          postalCode = Some("10001"),
          bestRepresentation = Some("Grocery Store - New York, NY")
        )
      ),
      checkNum = Some("0"),
      incomeType = Some(""),
      subaccountSecurityType = Some(""),
      commissionAmount = Some(0),
      splitDenominator = Some(0.0),
      splitNumerator = Some(0.0),
      sharesPerContract = Some(0.0),
      taxesAmount = Some(0),
      unitPrice = Some(0.0),
      currencySymbol = Some("$"),
      subAccountFund = Some(""),
      ticker = Some(""),
      securityId = Some(""),
      securityIdType = Some(""),
      investmentTransactionType = Some("cancel"),
      effectiveDate = Some(0L),
      firstEffectiveDate = Some(0L)
    ),
    Transaction(
      id = 9L,
      amount = -20.0,
      accountId = 123L,
      customerId = 789L,
      status = "Active",
      description = "Gas Station",
      createdDate = 1622548800L,
      memo = Some(""),
      `type` = Some("debit"),
      interestAmount = Some(0.0),
      principalAmount = Some(0.0),
      feeAmount = Some(0.0),
      escrowAmount = Some(0.0),
      unitQuantity = Some(0.0),
      postedDate = Some(1622793600L),
      transactionDate = Some(1622793600L),
      categorization = Some(
        Categorization(
          normalizedPayeeName = "Gas Station",
          category = "Transportation",
          country = "USA",
          city = Some("New York"),
          state = Some("NY"),
          postalCode = Some("10001"),
          bestRepresentation = Some("Gas Station - New York, NY")
        )
      ),
      checkNum = Some("0"),
      incomeType = Some(""),
      subaccountSecurityType = Some(""),
      commissionAmount = Some(0),
      splitDenominator = Some(0.0),
      splitNumerator = Some(0.0),
      sharesPerContract = Some(0.0),
      taxesAmount = Some(0),
      unitPrice = Some(0.0),
      currencySymbol = Some("$"),
      subAccountFund = Some(""),
      ticker = Some(""),
      securityId = Some(""),
      securityIdType = Some(""),
      investmentTransactionType = Some("contribution"),
      effectiveDate = Some(0L),
      firstEffectiveDate = Some(0L)
    )
  )

  val accountOwner = AccountOwner(
    ownerName = "John Doe",
    ownerAddress = "123 Main St"
  )

  val achDetail = ACHDetails(
    routingNumber = "987654321",
    realAccountNumber = "123456789"
  )

  val addCustomerRequest = NewCustomer(
    username = "jdoe",
    firstName = Some("John"),
    lastName = Some("Doe"),
    applicationId = None
  )

  val addCustomerResponse = CreatedCustomer(
    id = "123",
    username = "jdoe",
    createdDate = 1622548800L
  )

  val appFIStatus = AppFinancialInstitutionStatus(
    id = 1L,
    decryptionKeyActivated = true,
    createdDate = 1622548800L,
    lastModifiedDate = 1622548800L,
    status = true,
    abbrvName = Some("FIN"),
    logoUrl = None
  )

  val appRegistrationRequest = Application(
    appDescription = "An app that integrates with Finicity API",
    appName = "My Finicity App",
    appUrl = "https://myapp.com",
    ownerAddressLine1 = "1 App St",
    ownerAddressLine2 = "",
    ownerCity = "City",
    ownerCountry = "Country",
    ownerName = "Owner Name",
    ownerPostalCode = "00000",
    ownerState = "ST",
    image = ""
  )

  val appStatus = AppStatus(
    partnerId = "partner-1",
    preAppId = "1",
    appName = "My Finicity App",
    submittedDate = 1622548800L,
    modifiedDate = 1622548800L,
    status = "A",
    note = Some("All good"),
    applicationId = Some("app-1"),
    scopes = Some("all"),
    institutionDetails = Some(Seq(appFIStatus))
  )

  val appStatus2 = AppStatus(
    partnerId = "partner-1",
    preAppId = "2",
    appName = "My Finicity App",
    submittedDate = 1622548800L,
    modifiedDate = 1622548800L,
    status = "R",
    note = Some("Please contact support."),
    applicationId = None,
    scopes = None,
    institutionDetails = None
  )

  val assetSummaryAccountType = PrequalificationReportAssetSummary(
    `type` = Some("checking"),
    currentBalance = 1000.0,
    twoMonthAverage = 900.0,
    sixMonthAverage = Some(800.0),
    beginningBalance = 500.0
  )

}

trait Greeting {
  lazy val greeting: String = "hello"
}
