package example

import finicity.models.*

object FinicityExamples {
  // Core examples for common/generated Finicity models
  val exampleAccount: Account = Account(
    id = "acct-1",
    number = "000111222",
    name = "Example Account",
    balance = 1234.56,
    `type` = "checking",
    status = "active",
    customerId = "cust-1",
    institutionId = "inst-1",
    createdDate = 1622548800,
    currency = "USD",
    institutionLoginId = 1,
    displayPosition = 0
  )

  val exampleAccountDetail: AccountDetail = AccountDetail(
    interestMarginBalance = 0.0,
    availableCashBalance = 1234.56,
    vestedBalance = 0.0,
    currentLoanBalance = 0.0,
    availableBalanceAmount = 1234.56
  )

  val exampleCategorization: Categorization = Categorization(
    normalizedPayeeName = "Example Payee",
    category = "uncategorized",
    country = "USA",
    city = None,
    state = None,
    postalCode = None,
    bestRepresentation = None
  )

  val exampleTransaction: Transaction = Transaction(
    id = 1L,
    amount = -10.0,
    accountId = 1L,
    customerId = 1L,
    status = "active",
    description = "Test txn",
    memo = "",
    `type` = TransactionType.debit,
    interestAmount = 0.0,
    principalAmount = 0.0,
    feeAmount = 0.0,
    escrowAmount = 0.0,
    unitQuantity = 0,
    postedDate = java.time.LocalDate.now(),
    transactionDate = java.time.LocalDate.now(),
    createdDate = java.time.LocalDate.now(),
    categorization = exampleCategorization,
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
  )

  val exampleAccountIDs: AccountIDConsumerAttributes =
    AccountIDConsumerAttributes(
      accountIds = Seq("acct-1", "acct-2")
    )

  val exampleOwner: AccountOwnerv1 = AccountOwnerv1(
    ownerName = "John Doe",
    ownerAddress = "123 Main St"
  )

  val exampleACH: ACHDetails = ACHDetails(
    routingNumber = "111000025",
    realAccountNumber = "000111222"
  )

  val exampleAddCustomerReq: AddCustomerRequest = AddCustomerRequest(
    username = "jdoe",
    firstName = Some("John"),
    lastName = Some("Doe"),
    applicationId = None
  )

  val exampleAddCustomerResp: AddCustomerResponse = AddCustomerResponse(
    id = 1L,
    username = "jdoe",
    createdDate = java.time.Instant.now().toString
  )

  val exampleAppFIStatus: AppFIStatus = AppFIStatus(
    id = 1L,
    decryptionKeyActivated = true,
    createdDate = 1622548800L,
    lastModifiedDate = 1622548800L,
    status = true,
    abbrvName = Some("FIN"),
    logoUrl = None
  )

  val exampleAppRegistrationReq: AppRegistrationRequest =
    AppRegistrationRequest(
      appDescription = "Example app",
      appName = "Example",
      appUrl = "https://example.com",
      ownerAddressLine1 = "1 App St",
      ownerAddressLine2 = "",
      ownerCity = "City",
      ownerCountry = "Country",
      ownerName = "Owner",
      ownerPostalCode = "00000",
      ownerState = "ST",
      image = ""
    )

  val exampleAppRegistrationResp: AppRegistrationResponse =
    AppRegistrationResponse(
      preAppId = 1L,
      status = "P"
    )

  val exampleAppStatus: AppStatus = AppStatus(
    partnerId = "partner-1",
    preAppId = 1L,
    appName = "Example",
    submittedDate = 1622548800L,
    modifiedDate = 1622548800L,
    status = "A",
    note = None,
    applicationId = None,
    scopes = None,
    institutionDetails = None
  )

  val exampleAssetSummary: AssetSummaryAccountType = AssetSummaryAccountType(
    `type` = "checking",
    currentBalance = 1000.0,
    twoMonthAverage = 900.0,
    sixMonthAverage = 800.0,
    beginningBalance = 500.0
  )

}
