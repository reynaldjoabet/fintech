## Open Banking
Open Banking: This initiative allows third-party fintechs to securely access a customer's banking data with their permission. This fosters innovation and enables the creation of new financial services, like personal finance management tools and improved lending platforms.

### Card-based payments

- Flow: Customer => Merchant => Payment Gateway => Acquirer => Card Network (Visa/Mastercard) => Issuer Bank => Response.
Use cases: e-commerce checkout, POS, subscriptions.

- Gaps in traditional system:
 - High fees (interchange + scheme fees).
 - Declines (especially cross-border).
 - Fraud/chargebacks.

- How fintechs fill the gap:
 - Stripe/Adyen: developer-friendly APIs, smart retries, global acquiring.
 - Checkout.com: unified acceptance for global merchants.
 - Risk/fraud fintechs: advanced ML fraud prevention (e.g., Riskified, Forter).


### Bank transfers (ACH, SEPA, SWIFT)

- Flow: Customer => Bank (ACH/SEPA credit) => Clearing house => Recipient’s bank.
Use cases: payroll, bill payments, B2B, payouts.
- Gaps:
 - Slow (1–3 days).
 - Expensive cross-border (SWIFT fees + FX spreads).
 - Error-prone (wrong account numbers, missing references).

- Fintech gap fillers:
 - Wise (TransferWise): cheap FX, transparency, local banking rails.
 - Revolut/N26: instant peer transfers, multi-currency accounts.
 - Plaid/TrueLayer: APIs to initiate & verify bank transfers via open banking


 ### Instant / Real-time payments

Flow: Customer => Bank => Real-Time Payments Network (RTP, Faster Payments, UPI, Pix) => Recipient Bank => Instant settlement.

Use cases: gig worker payouts, P2P, merchant settlement.
- Gaps:
 - Limited adoption outside certain geographies.
 - Difficult for businesses to integrate directly with rails.
 - Risk of fraud (no chargebacks like cards).

- Fintech solutions:
 - Cash App, Zelle, Venmo: consumer P2P overlays.
 - Stripe Treasury, Payoneer: embedded payouts for platforms.
 - Banking-as-a-service (Marqeta, Unit, Solaris): give fintechs API access to RTP rails.

 ### Cross-border remittances

- Flow: Sender => Remittance provider => Local payout partner => Recipient.

Use cases: migrant workers, family support, B2B trade.
- Gaps:
 - Expensive fees (often 7–10%).
 - Long settlement times.
 - Limited transparency.

- Fintech solutions:
 - Wise, Remitly, Revolut: near real-time FX & transfers.
 - Ripple/Stellar: blockchain-based cross-border rails.


A regulatory and technical framework (starting in the EU/UK, now spreading worldwide) that requires banks to expose customer account and payment data to third parties via standardized APIs, but only with the customer’s explicit consent.

Goal: Increase competition and innovation in financial services by allowing fintechs to build products on top of bank data.

Example use case: A budgeting app that connects to your bank account to analyze your spending, or a payment initiation service that lets you pay directly from your bank account without a card.

`Open Banking is about data and payment access`

Banking-as-a-Service (BaaS)
A business model where licensed banks provide their regulated banking infrastructure (accounts, payments, cards, compliance, etc.) to third parties via APIs.

Goal: Allow fintechs or non-banks to embed full banking functionality into their apps without needing their own banking license.

Example use case: A ride-sharing company issuing debit cards to drivers, or a fintech launching a neobank app with savings accounts and payments using a partner bank’s infrastructure

```sh
Open Banking implies that banks are obliged to open their public APIs and allow third-party payment service providers to initiate payments and access the account information of their clients, subject to their consent. The Open Banking framework opens up plenty of opportunities for the Cameroonian financial market: credit and non-credit financial institutions, various payment service providers, payment agents, and e-money institutions.
```


### When config.json shines
- Portability across tools: The same config works with `openapi-generator-cli` (Docker/CLI), SBT, Maven, Gradle. This makes CI/CD or polyglot repos simpler.
- Single source of truth: All generator-specific knobs (`generatorName`, `jsonLibrary`, `additionalProperties`, vendor templates) live in one file that doesn’t depend on SBT syntax.
- Stable against plugin churn: Generator options evolve frequently. A JSON config won’t break if an SBT key is renamed or not exposed.
- Cleaner reviews: Spec + config diff separately from Scala build logic. Fewer noisy SBT reloads due to large setting blocks.
- Easy templating: You can keep multiple configs (e.g., `sttp3-circe.json`, `sttp4-jsoniter.json`) and switch by flipping a path.

`config.json (Scala 3 + sttp v4 + Circe)`

```json
{
  "generatorName": "scala-sttp4",
  "inputSpec": "modules/codegen-payroll/openapi/payroll.yaml",
  "outputDir": "modules/codegen-payroll/target/openapi",
  "mainPackage": "com.acme.payroll.client",
  "apiPackage": "com.acme.payroll.client.api",
  "modelPackage": "com.acme.payroll.client.model",
  "invokerPackage": "com.acme.payroll.client",
  "globalProperties": { "apis": "", "models": "", "supportingFiles": "" },
  "configOptions": {
    "useScala3": "true",
    "dateLibrary": "java8",
    "jsonLibrary": "circe",
    "modelPropertyNaming": "camelCase",
    "ensureUniqueParams": "true",
    "sortParamsByRequiredFlag": "true",
    "sortModelPropertiesByRequiredFlag": "true",
    "prependFormOrBodyParameters": "false",
    "allowUnicodeIdentifiers": "false",
    "sourceFolder": "src/main/scala",
    "hideGenerationTimestamp": "true",
    "separateErrorChannel": "false",
    "enumUnknownDefaultCase": "true",
    "disallowAdditionalPropertiesIfNotPresent": "false",
    "sttpClientVersion": "4.0.0"  // <- set to the exact version you use
  },
  "additionalProperties": {
    "artifactId": "payroll-client",
    "artifactVersion": "1.0.0"
  }
}
```

Final path = `openApiOutputDir / sourceFolder / <packages...>`
e.g. with
`openApiOutputDir := target/openapi` and `sourceFolder = "src/main/scala"` 
 files land in `target/openapi/src/main/scala/com/yourco/...`


`mainPackage` — the top-level/root package. If you don’t set the others, this value is used to define `apiPackage`, `modelPackage`, and `invokerPackage` (generator default root is org.openapitools.client). 
`apiPackage` — where the endpoint classes (one per tag, with methods for each operation) go. 
`modelPackage` — where the schema models (case classes/enums for request/response bodies) go. 
`invokerPackage` — the infrastructure/glue used by the client (common support code, request/response helpers, auth/serialization helpers, etc.); effectively the root package for generated code in many generators.


```json
{
  "generatorName": "scala-sttp4-jsoniter",
  "inputSpec": "modules/codegen-payroll/openapi/payroll.yaml",
  "outputDir": "modules/codegen-payroll/target/openapi",
  "mainPackage": "com.acme.payroll.client",
  "apiPackage": "com.acme.payroll.client.api",
  "modelPackage": "com.acme.payroll.client.model",
  "invokerPackage": "com.acme.payroll.client",
  "globalProperties": { "apis": "", "models": "", "supportingFiles": "" },
  "configOptions": {
    "useScala3": "true",
    "sourceFolder": "src/main/scala",
    "modelPropertyNaming": "camelCase",
    "ensureUniqueParams": "true",
    "sortParamsByRequiredFlag": "true",
    "sortModelPropertiesByRequiredFlag": "true",
    "allowUnicodeIdentifiers": "false",
    "prependFormOrBodyParameters": "false",

    "enumUnknownDefaultCase": "true",
    "disallowAdditionalPropertiesIfNotPresent": "false",
    "separateErrorChannel": "false",

    "sttpClientVersion": "4.0.11",
    "jsoniterVersion": "2.38.2"
  },
  "additionalProperties": {
    "artifactId": "payroll-client",
    "artifactVersion": "1.0.0"
  }
}
```
```sbt
lazy val codegenPayroll = project
  .in(file("modules/codegen-payroll"))
  .enablePlugins(OpenApiGeneratorPlugin)
  .settings(
    // === what was "generatorName", "inputSpec", "outputDir" ===
    openApiGeneratorName := "scala-sttp4-jsoniter",                                            // -g
    openApiInputSpec     := ((ThisBuild / baseDirectory).value / 
                              "modules" / "codegen-payroll" / "openapi" / "payroll.yaml").getPath,
    openApiOutputDir     := ((Compile / target).value / "openapi").getPath,

    // === what was "mainPackage"/"apiPackage"/"modelPackage"/"invokerPackage" ===
    // You can either set 'mainPackage' via additional properties (see below),
    // or set the 3 explicit package keys:
    openApiInvokerPackage := "com.acme.payroll.client",
    openApiApiPackage     := "com.acme.payroll.client.api",
    openApiModelPackage   := "com.acme.payroll.client.model",

    // === what was "globalProperties": { "apis": "", "models": "", "supportingFiles": "" } ===
    openApiGlobalProperties := Map(
      "apis"            -> "",
      "models"          -> "",
      "supportingFiles" -> ""
    ),

    // === what was "configOptions" / generator options ===
    // (In SBT, pass these via 'openApiAdditionalProperties')
    openApiAdditionalProperties ++= Map(
      "sourceFolder"                           -> "src/main/scala",
      "modelPropertyNaming"                    -> "camelCase",
      "ensureUniqueParams"                     -> "true",
      "sortParamsByRequiredFlag"               -> "true",
      "sortModelPropertiesByRequiredFlag"      -> "true",
      "allowUnicodeIdentifiers"                -> "false",
      "prependFormOrBodyParameters"            -> "false",

      // forward-compat & spec compliance
      "enumUnknownDefaultCase"                 -> "true",
      "disallowAdditionalPropertiesIfNotPresent" -> "false",

      // error model choice (Either in F, or raise via effect)
      "separateErrorChannel"                   -> "false",

      // pin runtime libs used by the templates
      "sttpClientVersion"                      -> "4.0.11",
      "jsoniterVersion"                        -> "2.38.2"

      // If you prefer one knob to set all packages, you may ALSO set:
      // "mainPackage" -> "com.acme.payroll.client"
      // (then you can drop the three *Package keys above)
    ),

    // validate spec & hook generation into compile
    openApiValidateSpec := Some(true),
    Compile / sourceGenerators += Def.task {
      openApiGenerate.value
      val out = file(openApiOutputDir.value) / "src" / "main" / "scala"
      (out ** "*.scala").get
    }.taskValue
  )
```
```sh
docker run --rm -v "$PWD":/local openapitools/openapi-generator-cli \
  generate -c /local/modules/codegen-payroll/openapi/config.json
```