package example

import finicity.models.*

object FinicityExamples {

  // Core examples for common/generated Finicity models
  val exampleAccount: CustomerAccount = CustomerAccount(
    id = "acct-1",
    number = "000111222",
    accountNumberDisplay = "000111222",
    name = "Example Account",
    balance = Some(1234.56),
    `type` = "checking",
    status = "active",
    customerId = "cust-1",
    institutionId = "inst-1",
    createdDate = 1622548800L,
    currency = "USD",
    institutionLoginId = 1,
    displayPosition = Some(0)
  )

  val exampleAccountDetail: AccountDetails = AccountDetails(
    interestMarginBalance = Some(0.0),
    availableCashBalance = Some(1234.56),
    vestedBalance = Some(0.0),
    currentLoanBalance = Some(0.0),
    availableBalanceAmount = Some(1234.56)
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
    createdDate = 1622548800L,
    memo = Some(""),
    `type` = Some("debit"),
    interestAmount = Some(0.0),
    principalAmount = Some(0.0),
    feeAmount = Some(0.0),
    escrowAmount = Some(0.0),
    unitQuantity = Some(0.0),
    postedDate = Some(1622548800L),
    transactionDate = Some(1622548800L),
    categorization = Some(exampleCategorization),
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
  )

  val exampleOwner: AccountOwner = AccountOwner(
    ownerName = "John Doe",
    ownerAddress = "123 Main St"
  )

  val exampleACH: ACHDetails = ACHDetails(
    routingNumber = "111000025",
    realAccountNumber = "000111222"
  )

  val exampleAddCustomerReq: NewCustomer = NewCustomer(
    username = "jdoe",
    firstName = Some("John"),
    lastName = Some("Doe"),
    applicationId = None
  )

  val exampleAddCustomerResp: CreatedCustomer = CreatedCustomer(
    id = "1",
    username = "jdoe",
    createdDate = 1622548800L
  )

  val exampleAppFIStatus: AppFinancialInstitutionStatus =
    AppFinancialInstitutionStatus(
      id = 1L,
      decryptionKeyActivated = true,
      createdDate = 1622548800L,
      lastModifiedDate = 1622548800L,
      status = true,
      abbrvName = Some("FIN"),
      logoUrl = None
    )

  val exampleAppRegistrationReq: Application =
    Application(
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

  val exampleAppStatus: AppStatus = AppStatus(
    partnerId = "partner-1",
    preAppId = "1",
    appName = "Example",
    submittedDate = 1622548800L,
    modifiedDate = 1622548800L,
    status = "A",
    note = None,
    applicationId = None,
    scopes = None,
    institutionDetails = None
  )

  val exampleAssetSummary: PrequalificationReportAssetSummary =
    PrequalificationReportAssetSummary(
      `type` = Some("checking"),
      currentBalance = 1000.0,
      twoMonthAverage = 900.0,
      sixMonthAverage = Some(800.0),
      beginningBalance = 500.0
    )

}
