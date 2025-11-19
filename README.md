# fintech

## Fintech (Financial Technology)

- Broad category that covers all uses of technology in financial services.
- Includes payments, but also things like:
  - Lending/credit (e.g. Klarna, LendingClub)
  - Banking tech (neobanks like Monzo, Chime)
  - Wealth/investing (Robinhood, Wealthfront)
  - Insurance tech (Lemonade, Hippo)
  - Compliance/regtech (identity verification, KYC, AML)
  - Crypto & blockchain

## Payments

- A subset of fintech, focused only on moving money between parties.

Examples:

- Card processing (Visa, Stripe)
- Mobile wallets (Apple Pay, Google Pay)
- Cross-border transfers (Wise, PayPal)
- Merchant acquiring / PoS


- 💳 Payments
  - Card processing (credit, debit, prepaid)
  - Mobile & digital wallets (Apple Pay, Google Pay, Samsung Pay)
  - Bank transfers (ACH, SEPA, SWIFT, RTP, UPI)
  - Peer-to-peer payments (Venmo, Cash App, PayPal)
  - Merchant acquiring & POS integration
  - Cross-border & remittances
  - Subscription & recurring billing
  - Refunds, chargebacks, and disputes
  - Payment gateways & APIs (Stripe, Adyen, Braintree, etc.)
  - Tokenization & PCI compliance

### Bank transfers:
- Batch (ACH–US, SEPA Credit–EU)
- Instant (Faster Payments–UK, RTP–US, UPI–India, Pix–Brazil)
- Wires (SWIFT/CHAPS/RTGS) – high value.
Wallets: Apple Pay, Google Pay, PayPal, local wallets (Alipay, M-Pesa, etc.).

## Consumer-to-Business (C2B):
The consumer provides payment information (e.g., credit card details) at the point of sale (POS) or online. The merchant's system sends this information to a payment gateway, which then routes it to a payment processor. The payment processor sends a request to the issuing bank (the consumer's bank) for authorization. Once authorized, the funds are transferred from the consumer's account to the merchant's acquiring bank, and finally to the merchant's account. This can take a few business days to settle


if you already have `api.domain.com`, don’t nest another `/api` in the path. Prefer:
`https://api.domain.com/v1/...` ✅
not `https://api.domain.com/api/v1/` 

### URL shape
Base: `https://api.domain.com/v1`
### Resources (plural nouns):
`/v1/users, /v1/users/{userId}`
`/v1/teams/{teamId}/members` (hierarchical when it adds clarity)

```sh
GET    /v1/users
POST   /v1/users
GET    /v1/users/{userId}
PATCH  /v1/users/{userId}
DELETE /v1/users/{userId}

GET    /v1/teams
POST   /v1/teams
GET    /v1/teams/{teamId}/members
POST   /v1/teams/{teamId}/members   (add)
DELETE /v1/teams/{teamId}/members/{userId} (remove)
```

### Documentation & discoverability
Provide an OpenAPI spec at `https://api.domain.com/openapi.json` and a human docs site.

### When to consider alternatives
If clients need lots of joins/shape control → consider GraphQL at `https://api.domain.com/graphql`.
If you need bi-directional push → Webhooks (`https://api.domain.com/v1/webhooks`) and/or WebSockets (`wss://api.domain.com/v1/events`)


Tenanting (if multi-tenant): prefer headers or claims; avoid putting tenant IDs in every path unless it’s core to the resource model.

### Tenant from JWT claims (best for end-user calls)
Clients authenticate with a Bearer token that already contains the tenant.
```http
Request
GET https://api.domain.com/v1/users
Authorization: Bearer eyJhbGciOi...
```

```json
{
  "sub": "usr_9s3...",
  "tid": "tn_12f4c0a9a3",        // tenant id (a.k.a. orgId, accountId)
  "scope": "users:read",
  "role": "member",
  "exp": 1750000000
}
```
