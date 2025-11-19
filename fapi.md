## FAPI

- Requiring Stronger Client Authentication: Instead of just using a client secret, FAPI mandates more robust methods like private key JSON Web Tokens (JWTs) or Mutual TLS (mTLS), where both the client and the server authenticate each other using digital certificates.

- Preventing Authorization Code Interception: It requires the use of Proof Key for Code Exchange (PKCE), which prevents an attacker from intercepting an authorization code and using it to get an access token.

- Enforcing Pushed Authorization Requests (PAR): This standard ensures that the parameters for an authorization request are sent directly to the authorization server via a secure backchannel, preventing them from being intercepted or tampered with in the user's browser.
- Mandating Sender-Constrained Tokens: FAPI ensures that an access token can only be used by the client that was issued to it. It does this by binding the token to the client's cryptographic key using mechanisms like mTLS or Demonstrating Proof-of-Possession (DPoP). This prevents a stolen token from being replayed by an attacker

## Examples
The primary use case for FAPI is Open Banking and Open Finance, where it is used to securely share financial data and initiate payments between banks and third-party applications (FinTechs) with a customer's explicit consent.

1. Open Banking in the UK: The UK's Open Banking Implementation Entity (OBIE) uses a FAPI-based profile to allow customers to securely share their bank account information with registered third-party providers (TPPs) for services like:

- Account Aggregation: A personal finance app (e.g., Monarch Money or Cleo) can get a customer's transaction history and balance from multiple banks, providing a consolidated view of their finances.

- Payment Initiation: A customer can approve a payment from their bank account directly through a merchant's app or website, bypassing traditional credit card payments. This is often faster and has lower fees.

2. Australia's Consumer Data Right (CDR): Australia's open data initiative for banking, energy, and telecommunications also uses a FAPI profile to ensure secure and standardized data sharing

3. E-Government and Healthcare: The security model of FAPI is not limited to finance. It is also being adopted for other high-value, sensitive data scenarios, such as:

- Secure Identity Verification: A government agency could use FAPI to verify a citizen's identity by having them log in through their bank, which already holds a high level of verified information.

- Secure Health Data Access: A patient could use a third-party app to securely access and share their health records with different healthcare providers.

```
Financial-grade API (FAPI) is a set of security and interoperability profiles that build on top of OAuth 2.0 and OpenID Connect to provide a higher level of security for high-value transactions and sensitive data. While OAuth and OpenID Connect offer flexibility, FAPI enforces specific, stricter requirements, making it ideal for industries that handle critical information.
```

Examples of FAPI Usage

## The primary application of FAPI is in the financial sector, but its strong security model is being adopted by other industries that deal with sensitive data.

Open Banking and Open Finance This is the most widespread use of FAPI. FAPI ensures that customers can securely share their financial data and initiate payments with third-party providers (TPPs) like FinTech apps and other banks.

- Account Aggregation: Apps like Mint or YNAB use FAPI to securely connect to a customer's bank accounts, credit cards, and investments to provide a single, consolidated view of their finances. The customer grants consent to the app, which then uses FAPI-compliant protocols to access the data without ever seeing the customer's bank credentials.

- Payment Initiation: A customer shopping online can choose to pay directly from their bank account instead of using a credit card. The merchant's system, acting as a TPP, uses FAPI to initiate a payment request with the customer's bank. The customer then authenticates and authorizes the payment securely through their bank's interface.


## E-Government

Governments handle a vast amount of sensitive personal data, from taxes to social security. FAPI can be used to securely manage these digital services.

- Digital Identity Verification: A citizen could use their bank's verified identity to securely log in to a government service, such as a tax portal or social benefits application. This leverages the strong authentication methods of FAPI to prevent identity fraud.

- Secure Data Sharing: FAPI could be used to enable the secure sharing of data between different government agencies with a citizen's consent. For example, a social services department could access a citizen's income data from the tax agency to verify eligibility for a program, all while maintaining strict data protection.


Major Banks: All of the UK's largest banks, including Barclays, Lloyds Banking Group, HSBC, and NatWest, have implemented FAPI-compliant APIs to enable third-party providers (TPPs) to securely access customer data and initiate payments.


Fintechs: Numerous TPPs, such as Moneyhub, Tink, and Plaid, use these FAPI-compliant APIs to offer services like account aggregation and personalized financial advice. For example, a customer can connect all their bank accounts and credit cards to an app to get a holistic view of their finances and spending habits.


FAPI typically focus on a key security feature it enforces: Pushed Authorization Requests (PAR). In a standard OAuth 2.0 flow, the authorization request parameters are sent via the browser's URL, which can be vulnerable to interception and tampering. FAPI's use of PAR addresses this by having the client application "push" the request parameters directly to the authorization server's secure endpoint via a back-channel POST request.
Request: GET request from the user's browser to the authorization endpoint.

### Standard OAuth 2.0 Authorization Request
```http
GET /authorize?
  client_id=s6BhdRkqt3&
  response_type=code&
  scope=openid%20profile%20payments&
  redirect_uri=https%3A%2F%2Fclient.example.com%2Fcb&
  state=xyz
HTTP/1.1
Host: server.example.com
```
The authorization server processes the request, the user logs in and consents, and the server redirects the user back to the client's redirect URI with an authorization code.

### FAPI-Compliant Authorization Request (using PAR)

A FAPI-compliant flow using PAR involves two main steps:

#### Step 1: Push the Authorization Request

The client application sends all the authorization parameters in a secure back-channel POST request to the `/par` (Pushed Authorization Requests) endpoint. This request includes the client's strong authentication, like a signed JWT.

Request: POST request directly from the client application to the `/par` endpoint.

```http
POST /par HTTP/1.1
Host: server.example.com
Content-Type: application/x-www-form-urlencoded
Authorization: Bearer <client_access_token>
client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer&
client_assertion=eyJraWQiOiJjbGllbnQt...&
client_id=s6BhdRkqt3&
response_type=code&
scope=openid%20profile%20payments&
redirect_uri=https%3A%2F%2Fclient.example.com%2Fcb&
state=xyz

```

Response: The authorization server validates the request, stores the parameters, and returns a unique `request_uri`

```json
HTTP/1.1 201 Created
Content-Type: application/json

{
  "request_uri": "urn:ietf:params:oauth:request_uri:bwc4JK-ESC0w8acc191e",
  "expires_in": 600
}
```

Code examples for FAPI typically focus on a key security feature it enforces: Pushed Authorization Requests (PAR). In a standard OAuth 2.0 flow, the authorization request parameters are sent via the browser's URL, which can be vulnerable to interception and tampering. FAPI's use of PAR addresses this by having the client application "push" the request parameters directly to the authorization server's secure endpoint via a back-channel POST request.

The code examples below illustrate the difference between a standard OAuth 2.0 flow and a FAPI-compliant flow using PAR. These are conceptual examples using curl to represent HTTP requests.

1. Standard OAuth 2.0 Authorization Request

In a standard flow, the client constructs a URL with all the request parameters and redirects the user's browser to it. This request is visible in the browser's address bar and history.

Request: GET request from the user's browser to the authorization endpoint.

GET /authorize?
  client_id=s6BhdRkqt3&
  response_type=code&
  scope=openid%20profile%20payments&
  redirect_uri=https%3A%2F%2Fclient.example.com%2Fcb&
  state=xyz
HTTP/1.1
Host: server.example.com

What happens next: The authorization server processes the request, the user logs in and consents, and the server redirects the user back to the client's redirect URI with an authorization code.

2. FAPI-Compliant Authorization Request (using PAR)

A FAPI-compliant flow using PAR involves two main steps:

Step 1: Push the Authorization Request

The client application sends all the authorization parameters in a secure back-channel POST request to the /par (Pushed Authorization Requests) endpoint. This request includes the client's strong authentication, like a signed JWT.

Request: POST request directly from the client application to the /par endpoint.

POST /par HTTP/1.1
Host: server.example.com
Content-Type: application/x-www-form-urlencoded
Authorization: Bearer <client_access_token>
client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer&
client_assertion=eyJraWQiOiJjbGllbnQt...&
client_id=s6BhdRkqt3&
response_type=code&
scope=openid%20profile%20payments&
redirect_uri=https%3A%2F%2Fclient.example.com%2Fcb&
state=xyz

Response: The authorization server validates the request, stores the parameters, and returns a unique request_uri.
JSON

HTTP/1.1 201 Created
Content-Type: application/json

{
  "request_uri": "urn:ietf:params:oauth:request_uri:bwc4JK-ESC0w8acc191e",
  "expires_in": 600
}

#### Step 2: Initiate the Authorization Flow

Now, the client initiates the browser-based authorization flow by redirecting the user, but instead of the full list of parameters, it only sends the client_id and the request_uri received in the previous step.

Request: GET request from the user's browser to the authorization endpoint.

```http
GET /authorize?
  client_id=s6BhdRkqt3&
  request_uri=urn:ietf:params:oauth:request_uri:bwc4JK-ESC0w8acc191e
HTTP/1.1
Host: server.example.com
```

The authorization server looks up the request parameters using the `request_uri`, processes the request, and proceeds with the user's authentication and consent. The flow continues securely, knowing the request parameters were validated in the back-channel.


[Barclays API Exchange Developer Portal](https://developer.barclays.com/)

[HSBC's Open Banking Page on openbanking.org.uk](https://www.openbanking.org.uk/regulated-providers/hsbc-uk-bank/)

[CBA's Open Banking Page](https://www.commbank.com.au/banking/open-banking.html)

Link for Developers: [CDR Developer Portal (ACCC)](https://consumerdataright.atlassian.net/wiki/spaces/DP/overview) - This is the central repository for all CDR technical standards, which are based on FAPI.

Japan: Digital-Only Banking

Japan is another country where FAPI is used, particularly by new, digital-first banks.

- Minna Bank: This digital-only bank in Japan adopted FAPI to provide secure open APIs for its Banking as a Service (BaaS) platform. Their decision to use FAPI was a strategic choice to meet high security standards from the outset.

`API is a security and interoperability profile for OAuth, and it was originally intended for use in open banking scenarios`
FAPI is a security profile
Open banking means that the financial institution you are banking with ,allows you to use the data with the bank; your transactions,the capability for payments etc ,to use with third party applications. This means apis and hence OAuth

2 challenges of using OAuth in open banking

1. Security( traditional OAuth 2 has some security issues)
2. OAuth is a framework, which means it is a toolset. You can build great solutions based on it and they all look similar but they are not the same. Meaning if you have 2 different OAuth deployments, t is very likely they do not work the same,so you have to adjust your code to make them work for those two different deployments. troublesome for open banking

in the EU, in 2018, there were 6000 banks became api providers. interoperability is really important

if A and B both use OAuth,there is no gurantee that both can talk to each other

if A and B support fapi, high interoperability


In the context of OAuth 2.0 and OpenID Connect, a profile is a specific set of rules, requirements, and best practices built on top of the base protocol. Think of the core specifications as a flexible framework with many optional features. A profile narrows down these options to ensure interoperability and/or enforce a higher level of security for a specific use case.

In the context of OAuth 2.0 and OpenID Connect, a profile is a specific set of rules, requirements, and best practices built on top of the base protocol. Think of the core specifications as a flexible framework with many optional features. A profile narrows down these options to ensure interoperability and/or enforce a higher level of security for a specific use case.

## Why Profiles are Necessary

The OAuth 2.0 and OpenID Connect specifications are intentionally broad to support a wide range of applications, from simple mobile apps to complex enterprise systems. This flexibility, however, can lead to:

- Insecurity: Some grant types or options are less secure than others. A profile can mandate the use of only the most secure options, such as the Authorization Code flow with PKCE.

- Interoperability Issues: When different developers choose different optional features, their systems may not work together seamlessly. A profile solves this by requiring a common, agreed-upon set of features.

`By defining a profile, a community or industry can create a standard that everyone must follow. This ensures that a client application built by one company can securely and reliably interact with an authorization server from another`


## Examples of Profiles

### OpenID Connect

The OpenID Connect Core specification is itself a profile of the OAuth 2.0 framework, adding an identity layer. Within the OpenID Connect ecosystem, there are even more specific profiles for different contexts

- Financial-grade API (FAPI): This is a profile for OpenID Connect and OAuth 2.0 specifically designed for high-value transactions and sensitive data, like in the financial sector. It mandates very strict security requirements, such as client-authenticated JWTs and Pushed Authorization Requests (PAR), to prevent common attacks and ensure a high degree of trust.

- eKYC Profile: This profile is a set of standards for electronic Know Your Customer (eKYC) verification. It specifies how identity data should be securely shared between a verified identity provider and a service provider.

### OAuth 2.0

While OpenID Connect has many profiles, OAuth 2.0 also has its own profiles and related specifications.

- OAuth 2.0 Security Best Current Practice: This isn't a single profile but a document that recommends which parts of the base specification to use. For example, it strongly recommends that all clients use the Authorization Code flow with PKCE because it's the most secure. Many profiles, including FAPI, are based on these recommendations.

- OAuth 2.0 for Browser-Based Apps: This profile, or "best practice" document, provides guidance on how to securely implement OAuth 2.0 for single-page applications (SPAs) running in a web browser.

- OAuth 2.0 for Native Apps: A similar profile that specifies how to implement OAuth 2.0 on native mobile applications, with a focus on mitigating security risks unique to those environments.


[FAPI 2.0 Security Profile](https://openid.net/specs/fapi-security-profile-2_0-final.html)
Abstract

OIDF FAPI 2.0 is an API security profile suitable for high-security applications based on the OAuth 2.0 Authorization Framework [RFC6749].


In OAuth 2.0 and FAPI (Financial-grade API), the term profile has a specific meaning:

A profile is a set of rules, constraints, and extensions applied to a base specification (like OAuth 2.0 or OpenID Connect) for a particular use case, industry, or level of security.

### In OAuth

The OAuth 2.0 framework is very broad and allows lots of optional behaviors.

To ensure interoperability and security, different communities define profiles.

Example: OpenID Connect itself is often referred to as a "profile" of OAuth 2.0 that adds authentication on top.

### In FAPI (Financial-grade API)
FAPI is an OAuth 2.0 + OIDC profile designed by the OpenID Foundation for high-security financial services (like Open Banking).

It defines two profiles:
- FAPI Read-Only Profile (for secure data access, e.g., account info).

- FAPI Read & Write Profile (for transactional APIs, e.g., payments).



```sh
The Financial Grade API (FAPI) profile is a layer on top of OAuth 2.0 and OpenID Connect which
“hardens” OAuth / OpenID Connect by specifying a set of constraints - called a profile - that limit or
enforce the alternatives provided by OAuth / OIDC.
```

FAPI as a way to implement OAuth/OIDC securely
FAPI as a way for fintechs(clients) to connect to bank APIs

- Banks (resource servers + authorization servers) implement OAuth/OIDC using FAPI.

- Fintechs (clients/third-party providers) consume those APIs using the same FAPI profile.

- This ensures interoperability and security guarantees across the ecosystem.


`A profile in OAuth 2.0 or FAPI is a specific set of rules that defines how a general protocol should be implemented for a particular use case. Think of it as a template or a checklist of mandatory and optional features.`

The Role of Profiles

The core OAuth 2.0 and OpenID Connect specifications are intentionally flexible to be used in various scenarios. For example, some features are optional, some are more secure than others, and some are specific to certain environments (like a web browser vs. a mobile app). This flexibility, however, can lead to:

- Insecurity: Without a clear set of guidelines, developers might choose less secure options, making a system vulnerable.

- Interoperability Issues: If every organization implements the protocol differently, their systems won't be able to communicate with each other effectively.

`A profile solves these problems by providing a standardized, secure, and interoperable way to implement the protocol. It reduces the number of options available and enforces the use of specific, high-security features`


**FAPI 1.0**: This version had two main profiles:

- Baseline: Designed for low-risk scenarios (e.g., read-only data).

- Advanced: A much stricter profile for high-risk scenarios (e.g., payments and read/write access). It mandates advanced security features like client authentication using private key JWTs and Pushed Authorization Requests (PAR).

**FAPI 2.0**: This is the latest version, which simplifies the FAPI profiles and makes them more versatile. It has a single Security Profile that is designed to be secure enough for most high-value use cases. This version emphasizes simplicity and ease of implementation while maintaining strong security.



FAPI's Impact on the Payment Process

FAPI fundamentally changes the payment flow by enforcing a higher standard of security and shifting control to the consumer and their bank.

Direct Account-to-Account Payments: The payment is made directly from the customer's bank account to the merchant's bank account, bypassing traditional card networks.

Enhanced Security: FAPI's strict security requirements prevent common attacks. It mandates strong client authentication (e.g., using private keys), sender-constrained tokens to prevent theft, and Pushed Authorization Requests (PAR) so no sensitive information is ever passed through the user's browser. The customer never shares their banking credentials or card details with the PISP or the merchant. The customer's bank handles all authentication and consent, which is a much more secure process


Consumer Control: FAPI empowers the consumer with granular control over their data. They can grant a PISP permission to initiate a single payment, set a payment limit, or revoke consent at any time, all managed through their bank's secure interface


`This document specifies the process for a client to obtain sender-constrained tokens from an authorization server and use them securely with resource servers`

While the security profile was initially developed with a focus on financial applications, it is designed to be universally applicable for protecting APIs exposing high-value and sensitive (personal and other) data, for example, in e-health and e-government applications.

This document specifies the requirements for:
- Confidential clients to securely obtain OAuth tokens from authorization servers;
- Confidential clients to securely use those tokens to access protected resources at resource servers;
- Authorization servers to securely issue OAuth tokens to confidential clients;
- Resource servers to securely accept and verify OAuth tokens from confidential clients.