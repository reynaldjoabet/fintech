package example
import finicity.models.*
object Hello extends Greeting with App {
  println(greeting)

  val checkingAccount = Account(
    id = "123",
    number = "456",
    name = "My Account",
    balance = 1000.0,
    `type` = "Checking",
    status = "Active",
    customerId = "789",
    institutionId = "101112",
    createdDate = 1622548800,
    currency = "USD",
    institutionLoginId = 131415,
    displayPosition = 1
  )

  val savingsAccount = Account(
    id = "124",
    number = "457",
    name = "My Savings Account",
    balance = 5000.0,
    `type` = "Savings",
    status = "Active",
    customerId = "789",
    institutionId = "101112",
    createdDate = 1622548800,
    currency = "USD",
    institutionLoginId = 131415,
    displayPosition = 2
  )

  val accountDetail = AccountDetail(
    interestMarginBalance = 0.0,
    availableCashBalance = 1000.0,
    vestedBalance = 1000.0,
    currentLoanBalance = 0.0,
    availableBalanceAmount = 1000.0
  )

  val transactions = List(
    Transaction(
      id = 8L,
      amount = -50.0,
      accountId = 123L,
      customerId = 789L,
      status = "Active",
      description = "Grocery Store",
      memo = "",
      `type` = TransactionType.debit,
      interestAmount = 0.0,
      principalAmount = 0.0,
      feeAmount = 0.0,
      escrowAmount = 0.0,
      unitQuantity = 0,
      postedDate = java.time.LocalDate.of(2021, 6, 3),
      transactionDate = java.time.LocalDate.of(2021, 6, 3),
      createdDate = java.time.LocalDate.of(2021, 6, 3),
      categorization = Categorization(
        normalizedPayeeName = "Grocery Store",
        category = "Food",
        country = "USA",
        city = Some("New York"),
        state = Some("NY"),
        postalCode = Some("10001"),
        bestRepresentation = Some("Grocery Store - New York, NY")
      ),
      checkNum = 0,
      incomeType = "",
      subaccountSecurityType = "",
      commissionAmount = 0.0,
      splitDenominator = 0.0,
      splitNumerator = 0.0,
      sharesPerContract = 0.0,
      taxesAmount = 0.0,
      unitPrice = 0.0,
      currencySymbol = "$",
      subAccountFund = "",
      ticker = "",
      securityId = "",
      securityIdType = "",
      investmentTransactionType = InvestmentTransactionTypes.cancel,
      effectiveDate = "",
      firstEffectiveDate = ""
    ),
    Transaction(
      id = 9L,
      amount = -20.0,
      accountId = 123L,
      customerId = 789L,
      status = "Active",
      description = "Gas Station",
      memo = "",
      `type` = TransactionType.debit,
      interestAmount = 0.0,
      principalAmount = 0.0,
      feeAmount = 0.0,
      escrowAmount = 0.0,
      unitQuantity = 0,
      postedDate = java.time.LocalDate.of(2021, 6, 4),
      transactionDate = java.time.LocalDate.of(2021, 6, 4),
      createdDate = java.time.LocalDate.of(2021, 6, 4),
      categorization = Categorization(
        normalizedPayeeName = "Gas Station",
        category = "Transportation",
        country = "USA",
        city = Some("New York"),
        state = Some("NY"),
        postalCode = Some("10001"),
        bestRepresentation = Some("Gas Station - New York, NY")
      ),
      checkNum = 0,
      incomeType = "",
      subaccountSecurityType = "",
      commissionAmount = 0.0,
      splitDenominator = 0.0,
      splitNumerator = 0.0,
      sharesPerContract = 0.0,
      taxesAmount = 0.0,
      unitPrice = 0.0,
      currencySymbol = "$",
      subAccountFund = "",
      ticker = "",
      securityId = "",
      securityIdType = "",
      investmentTransactionType = InvestmentTransactionTypes.contribution,
      effectiveDate = "",
      firstEffectiveDate = ""
    )
  )

// check finicity.models and create an instance of all classes in that package, using dummy data for the fields. Print the instances to the console.

  val accountIDConsumerAttributes = AccountIDConsumerAttributes(
    accountIds = Seq("123", "124")
  )

  val accountIDConsumerAttributes2 = AccountIDConsumerAttributes(
    accountIds = Seq("125", "126")
  )

  val accountOwner = AccountOwnerv1(
    ownerName = "John Doe",
    ownerAddress = "123 Main St"
  )

  val accountType = AccountType.`401a`

  val accountType2 = AccountType.`401k`

  val accountType3 = AccountType.`403b`

  val accountType4 = AccountType.`457plan`

  val accountType5 = AccountType.ira
  val accountType6 = AccountType.`529plan`
  val accountType7 = AccountType.checking

  val accountType8 = AccountType.savings

  val accountType9 = AccountType.moneyMarket

  val accountType10 = AccountType.creditCard

  val accountType11 = AccountType.loan

  val accountType12 = AccountType.investment

  val accountType13 = AccountType.investmentTaxDeferred

  val achDetail = ACHDetails(
    routingNumber = "987654321",
    realAccountNumber = "123456789"
  )

  val addCustomerRequest = AddCustomerRequest(
    username = "jdoe",
    firstName = Some("John"),
    lastName = Some("Doe"),
    applicationId = None
  )

  val addCustomerResponse = AddCustomerResponse(
    id = 123L,
    username = "jdoe",
    createdDate = java.time.Instant.now().toString
  )

  val appFIStatus = AppFIStatus(
    id = 1L,
    decryptionKeyActivated = true,
    createdDate = 1622548800L,
    lastModifiedDate = 1622548800L,
    status = true,
    abbrvName = Some("FIN"),
    logoUrl = None
  )

  val appRegistrationRequest = AppRegistrationRequest(
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

  val appRegistrationResponse = AppRegistrationResponse(
    preAppId = 123L,
    status = "A"
  )

  val appStatus = AppStatus(
    partnerId = "partner-1",
    preAppId = 1L,
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
    preAppId = 2L,
    appName = "My Finicity App",
    submittedDate = 1622548800L,
    modifiedDate = 1622548800L,
    status = "R",
    note = Some("Please contact support."),
    applicationId = None,
    scopes = None,
    institutionDetails = None
  )

  val assetSummaryAccountType = AssetSummaryAccountType(
    `type` = "checking",
    currentBalance = 1000.0,
    twoMonthAverage = 900.0,
    sixMonthAverage = 800.0,
    beginningBalance = 500.0
  )
}

trait Greeting {
  lazy val greeting: String = "hello"
}
