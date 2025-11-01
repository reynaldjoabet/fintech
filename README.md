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

### Tenant from a header (for service-to-service)
Allow only trusted service principals (e.g., machine tokens, mTLS) to specify the tenant via a header such as `Tenant-Id` (or` X-Tenant-Id`). End-user tokens should not be allowed to override tenant by header.
```http
Request (service principal acting for a tenant)
POST https://api.domain.com/v1/invoices
Authorization: Bearer <svc_token_with_scope tenants:write_any>
Tenant-Id: tn_12f4c0a9a3
Content-Type: application/json

{ "amount": 12900, "currency": "USD" }

```

for `wss://api.domain.com` and  `https://api.domain.com`
Cloudflare sees only hostnames (DNS records), not protocols.
You add `api.domain.com` in DNS and `proxy it (orange cloud)`

Cloudflare automatically handles both `https://api.domain.com/...` and `wss://api.domain.com/...`

`When you add a DNS record in Cloudflare, each record has a little cloud icon`:
- Orange cloud ☁️ (proxied) : 
  - Traffic to this hostname goes through Cloudflare’s proxy network.
  - Cloudflare terminates TLS (SSL), applies security features (WAF, DDoS protection, bot management), caching, and then forwards the request to your origin server.
  - Works for both https:// and wss:// traffic.
- Gray cloud ☁️ (DNS only) :
  - Cloudflare only provides DNS resolution.
  - Traffic goes directly to your origin server, bypassing Cloudflare’s proxy.
  - You don’t get Cloudflare’s SSL, WAF, or DDoS protection on this hostname

  modern CDNs (Cloudflare, Fastly, Akamai, etc.) use Anycast to route traffic to their edge servers.

### SemanticDB

The support of SemanticDB is now shipped into the Scala 3 compiler:
- The `-Ysemanticdb` option activates the generation of semanticDB files.
- The `-semanticdb-target` option can be used to specify the output directory of semanticDB files.

sbt is able to configure SemanticDB automatically with this single setting: `semanticdbEnabled := true`.


In HTTP, status code 101 means "Switching Protocols".

- When a client (like a web browser) makes a request to a server, it can include a header called Upgrade to request a change to a different protocol.
- If the server agrees, it responds with HTTP/1.1 101 Switching Protocols and then changes the communication protocol for that connection.

## Example Use Cases
- WebSockets: When a client wants to upgrade from HTTP to WebSocket, the server will respond with 101 Switching Protocols if it supports WebSockets.
- HTTP/2 upgrade: Similarly, the server might upgrade from HTTP/1.1 to HTTP/2 if both sides agree.

```http
HTTP/1.1 101 Switching Protocols
Upgrade: websocket
Connection: Upgrade
```


`build an api-first bank from scratch that handles core banking,payments and compliance system`


nearly all of their functionality (accounts, payments, KYC, ledger, etc.) is exposed via APIs, allowing third parties (fintechs, platforms, apps) to integrate them deeply



Use https://api.domain.com.
Prefer Bearer tokens (OAuth 2.0 / OIDC) over cookies for API calls.
Lock cookies (if any) to Domain=api.domain.com and do not set them for .domain.com.
Tight CORS (explicit origin list, no wildcards with credentials, allow only needed headers/methods).
Add WAF, DDoS/rate limiting, separate monitoring & deployment

Use Content Security Policy (CSP) on the web app.
Set X-Content-Type-Options: nosniff, Referrer-Policy, Permissions-Policy.


Frontend: https://domain.com (static site/SPA/SSR behind CDN).
Public API: https://api.domain.com (separate stack, WAF, rate limits, independent deploys).
Auth:
Browser → API: Auth Code + PKCE (tokens stored in memory, not localStorage).
Server→API or partner clients: Client Credentials.
Tokens: Short-lived access tokens (5–15 min) + refresh via a backend or a well-audited token manager. Validate issuer, audience, alg, exp, kid.

```conf
# /etc/nginx/conf.d/api.conf
map $http_origin $cors_allow_origin {
    default "";
    "~^https://domain\.com$" "$http_origin";
    "~^https://www\.domain\.com$" "$http_origin";
}

server {
    listen 443 ssl http2;
    server_name api.domain.com;

    # ... TLS config ...

    # Preflight
    if ($request_method = OPTIONS) {
        add_header Access-Control-Allow-Origin $cors_allow_origin;
        add_header Access-Control-Allow-Methods "GET,POST,PUT,PATCH,DELETE";
        add_header Access-Control-Allow-Headers "Authorization,Content-Type";
        add_header Access-Control-Max-Age 600;
        add_header Vary "Origin";
        return 204;
    }

    # Actual responses
    location / {
        # your upstream/proxy_pass here

        # Set CORS only when origin is allowed
        if ($cors_allow_origin != "") {
            add_header Access-Control-Allow-Origin $cors_allow_origin;
            add_header Vary "Origin";
            # Only enable credentials if you must send cookies or auth via fetch with credentials
            # add_header Access-Control-Allow-Credentials "true";
        }

        # Security/behavioral headers for APIs
        add_header X-Content-Type-Options nosniff;
        add_header Referrer-Policy no-referrer;
        add_header Cache-Control "no-store"; # or appropriate per-resource
        add_header Content-Type "application/json; charset=utf-8";
    }
}
```

Set a strong CSP on domain.com (affects the app, not the API):
Example: only allow scripts from self and your CDN/IdP.

```c#
// Program.cs (ASP.NET Core 8+)
using Microsoft.AspNetCore.Authorization;
using Microsoft.IdentityModel.Tokens;

var builder = WebApplication.CreateBuilder(args);

builder.Services
    .AddAuthentication("Bearer")
    .AddJwtBearer("Bearer", options =>
    {
        options.Authority = "https://YOUR-IDP-ISSUER";   // ends without trailing slash for MS/Okta, with for some IdPs
        options.Audience  = "https://api.domain.com";    // your API identifier
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuer = true,
            ValidIssuer = "https://YOUR-IDP-ISSUER",
            ValidateAudience = true,
            ValidAudience = "https://api.domain.com",
            ValidateLifetime = true,
            ValidateIssuerSigningKey = true,
            // Restrict algs if your IdP supports it
            ValidAlgorithms = new[] { "RS256", "ES256" },
            // Clock skew if needed:
            ClockSkew = TimeSpan.FromSeconds(60)
        };
        // JWKS is pulled from the OIDC metadata automatically
        options.MapInboundClaims = false; // keep raw claim names (e.g., "scope")
    });

builder.Services.AddAuthorization(options =>
{
    options.AddPolicy("orders.read", policy =>
        policy.RequireAssertion(ctx =>
        {
            var scope = ctx.User.FindFirst("scope")?.Value ?? "";
            var scopes = scope.Split(' ', StringSplitOptions.RemoveEmptyEntries);
            // Some IdPs (Azure AD) put scopes in "scp"
            var scp = ctx.User.FindFirst("scp")?.Value?.Split(' ', StringSplitOptions.RemoveEmptyEntries) ?? Array.Empty<string>();
            return scopes.Contains("orders.read") || scp.Contains("orders.read");
        })
    );

    options.AddPolicy("orders.write", policy =>
        policy.RequireAssertion(ctx =>
        {
            var scope = ctx.User.FindFirst("scope")?.Value ?? "";
            var scopes = scope.Split(' ', StringSplitOptions.RemoveEmptyEntries);
            var scp = ctx.User.FindFirst("scp")?.Value?.Split(' ', StringSplitOptions.RemoveEmptyEntries) ?? Array.Empty<string>();
            return scopes.Contains("orders.write") || scp.Contains("orders.write");
        })
    );
});

var app = builder.Build();

app.UseAuthentication();
app.UseAuthorization();

app.MapGet("/v1/orders", [Authorize(Policy = "orders.read")] () => Results.Ok(new { ok = true }));
app.MapPost("/v1/orders", [Authorize(Policy = "orders.write")] () => Results.Ok(new { ok = true }));

app.Run();
```

`Content Security Policy (CSP)` is an HTTP response header (or <meta> tag) that tells the browser which sources of content are allowed to load on your site — and blocks everything else.

```sh
Content-Security-Policy: script-src 'self' https://cdn.domain.com https://login.identityprovider.com; object-src 'none'; base-uri 'self'; frame-ancestors 'none';
```

This means:
- Only load JavaScript from your own origin ('self'), your CDN, and your identity provider (IdP).
- Block inline <script> tags unless explicitly allowed.
- Block loading plugins (object-src 'none').
- Prevent clickjacking (frame-ancestors 'none').

`Your frontend app (domain.com) is your attack surface — not your API`.
If a malicious script sneaks in via a vulnerable dependency, third-party widget, or injection (e.g. stored XSS), it could:
- Steal access tokens or session cookies.
- Inject fake forms to capture credentials.
- Make unauthorized API calls to api.domain.com.
A tight CSP prevents most of those by restricting where scripts, styles, images, and frames can come from

### practical CSP for a modern SPA
If your frontend uses:
Scripts from your app and CDN,
Fonts from Google Fonts,
Calls your backend API at https://api.domain.com,
And uses Auth0 / Azure AD for login,
then you might use:

```sh
Content-Security-Policy:
  default-src 'none';
  script-src 'self' https://cdn.domain.com https://cdn.auth0.com;
  connect-src 'self' https://api.domain.com https://YOUR-IDP-DOMAIN;
  img-src 'self' data:;
  style-src 'self' https://fonts.googleapis.com 'unsafe-inline';
  font-src https://fonts.gstatic.com;
  frame-ancestors 'none';
  base-uri 'self';
```

### Deploying CSP
You can set it either:
In your reverse proxy / CDN / server:
`add_header Content-Security-Policy "script-src 'self' https://cdn.domain.com ..." always;`
Or in your app:
`<meta http-equiv="Content-Security-Policy" content="script-src 'self' ...">`

Use the header form whenever possible — it’s more secure and can’t be overridden by the page.

credit institution providing BaaS (bank accounts, cards, payments, lending, KYC) via APIs

### Payments platform
- Orchestration service
Flow: Intent → Authorize → Capture/Settle → Notify → Reconcile

### Domains & environments
Public (prod)
https://domain.com – marketing / docs landing (no PII)
https://docs.domain.com – API docs & changelog
https://status.domain.com – uptime & incidents
https://console.domain.com – partner/admin console (MFA required)
https://auth.domain.com – OAuth2/OIDC (Authorization Server)
https://api.domain.com – public API v1 (JSON over HTTPS)
https://events.domain.com – webhook delivery IPs (outbound)
https://sandbox.api.domain.com – public sandbox API
https://sandbox.auth.domain.com – sandbox IdP


## Public API (api.domain.com/v1)
1) Customers & KYC/KYB
Customers
POST /customers (scope: customers.write) – create person/business
GET /customers/{customerId} (customers.read)
PATCH /customers/{customerId} (customers.write) – updates (addr, phone)
POST /customers/{customerId}:close (customers.write)
KYC/KYB
POST /customers/{customerId}/kyc-intents (kyc.write) – start verification
GET /kyc-intents/{intentId} (kyc.read) – status: pending|needs_info|verified|rejected
POST /kyc-intents/{intentId}/documents (kyc.write; multipart) – doc upload
GET /customers/{customerId}/screenings (kyc.read) – PEP/sanctions hits & decisions
2) Accounts & products
Products
GET /products (products.read) – list product definitions (DDA, savings, wallet)
GET /products/{productId} (products.read)
Accounts
POST /accounts (accounts.write) – open account (customerId, productId, currency)
GET /accounts?customer_id=... (accounts.read)
GET /accounts/{accountId} (accounts.read)
PATCH /accounts/{accountId} (accounts.write) – freeze/unfreeze, labels
POST /accounts/{accountId}:close (accounts.write)
Balances & statements
GET /accounts/{accountId}/balance (accounts.read) – computed from ledger
GET /accounts/{accountId}/statements?from=...&to=...&format=pdf|csv (accounts.read)
Beneficiaries
POST /beneficiaries (beneficiaries.write)
GET /beneficiaries (beneficiaries.read)

3) Ledger (double-entry source of truth)
Transactions
GET /ledger/accounts/{accountId}/transactions?cursor=...&limit=... (ledger.read)
GET /ledger/journals/{journalId} (ledger.read)
Posting (internal or privileged partners)
POST /ledger/journals (ledger.post)

```http
{
  "type":"TRANSFER",
  "entries":[
    {"debitAccountId":"acc_src","creditAccountId":"acc_dst","amount":{"amount":50000,"currency":"ZAR"},"valueDate":"2025-10-13","externalRef":"pm_123"}
  ],
  "metadata":{"clientRef":"inv-8891"}
}
```
Reversals
POST /ledger/journals/{journalId}:reverse (ledger.post)

4) Payments (orchestration + rails)
Payment Intents (generic)
POST /payments/intents (payments.write) – create intent (source, destination, scheme, amount)
POST /payments/intents/{id}/confirm (payments.write) – execute
GET /payments/intents/{id} (payments.read) – state: pending|processing|succeeded|failed
Transfers (account-to-account / internal)
POST /transfers (payments.write) – instant book + ledger post
GET /transfers/{id} (payments.read)
Payouts (external rails)
POST /payouts (payments.write) – to bank acct/mobile money/card
GET /payouts/{id} (payments.read)
POST /payouts/{id}:cancel (payments.write) (if rail supports)
Collections (pull)
POST /debits (payments.write) – create debit against mandate
GET /debits/{id} (payments.read)
Mandates
POST /mandates (mandates.write) – create & sign mandate
GET /mandates/{id} (mandates.read)
Quotes & FX (optional)
POST /fx/quotes (fx.read) – price a currency conversion
POST /fx/conversions (fx.write) – book conversion
GET /fx/conversions/{id} (fx.read)
Rails metadata
GET /rails (payments.read) – supported schemes & capabilities per country (e.g., ZA EFT, KE mobile money)
Webhook events (outbound)
payment.succeeded, payment.failed, transfer.posted, payout.settled, mandate.signed

5) Webhooks (management)
POST /webhooks/endpoints (webhooks.write) – register URL & secret
GET /webhooks/endpoints (webhooks.read)
POST /webhooks/endpoints/{id}/rotate-secret (webhooks.write)
GET /webhooks/deliveries?endpoint_id=... (webhooks.read)
Headers we send:
Webhook-Id, Webhook-Timestamp, Webhook-Signature (HMAC-SHA256), Idempotency-Key
Retry: exponential backoff, up to N times; replay-protection via timestamp tolerance.

6) Compliance (screening, monitoring, cases)
Screening (on demand/ongoing)
POST /compliance/screenings (compliance.write) – run PEP/sanctions screening for an entity
GET /compliance/screenings/{id} (compliance.read)
Transaction Monitoring
GET /compliance/rules (compliance.read)
POST /compliance/rules (compliance.write) – create/edit rule (velocity, geo, amount patterns)
GET /compliance/alerts?status=open (compliance.read)
POST /compliance/alerts/{alertId}/dispositions (compliance.write) – escalate/close, add notes, attach docs
Cases & reporting
GET /compliance/cases/{caseId}
POST /compliance/reports/sar (compliance.write) – prepare SAR/STR draft (jurisdiction aware)


Many modules depend on many others (payments → core, accounts, customers; ledgers → core, accounts, customers). That grows a dense graph and slows builds.


[](https://koreminds.ai/a-guide-to-api-first-banking-and-open-finance/)


CockroachDB,postgres,yugabytedb

Key Requirements for Databases in Fintech
Here are major demands fintech systems place on their data layer:
- ACID transactions / strong consistency — for core banking, payments, ledgers. 
ResearchGate
+2
ResearchGate
+2
- High throughput & low latency — for trading, payments, real-time decisioning. 
Comprinno Tech
+1
- Scalability (read & write, distributed geography) — fintech systems often span regions and large user bases. 
prove.com
+1
- Flexibility in schema / handling semi-structured & unstructured data — e.g., customer behaviour, logs, risk-analytics. 
Techloy
+1
- Regulatory compliance, auditability, security — ledger trails, data locality, encryption. 
Vela - Postgres Database Platform
+1
- Real-time analytics, time-series/historical data — market data, trading ticks, risk exposures. 
Wikipedia
+1
Because of that mix, fintech systems often adopt polyglot persistence — i.e., more than one database type depending on use-case.

Role of Finance database
● The Golden Source: Serves as the single source of truth for external
regulatory, market, and financial reporting.
● Operational Backbone: Powers internal operational controls, reconciliations,
and ensures smooth financial workflows.
● Critical for Safeguarding: Used as the primary data source for safeguarding
customer funds and ensuring compliance with financial regulations.

This means...
Data must be always available, accurate, and
reliable

Database will be your bottleneck

In nginx, an upstream block defines a group of backend servers (e.g. your API, app, or auth service).
Each server line inside tells nginx where to forward requests — by IP address + port or a UNIX socket path

```sh
upstream api_backend {
    server 10.0.1.5:8080;     # backend server 1 (IP:PORT)
    server 10.0.1.6:8080;     # backend server 2 (for load balancing)
    keepalive 64;
}

```

Then elsewhere in your config:

```sh
location / {
    proxy_pass http://api_backend;
}
```

So when a request hits `https://api.domain.com`, nginx forwards it to one of those two internal servers on port 8080

There are two parts in your nginx config that work together:
A server block that matches the hostname (server_name api.domain.com;)
A proxy_pass directive that points to an upstream (e.g. proxy_pass http://api_backend;)

- A client (e.g. a browser or API client) sends:
```http
GET https://api.domain.com/v1/users
```
nginx looks at the Host header and sees `api.domain.com`
- nginx finds the right server block
It searches for a server block like:
```sh
server {
    listen 443 ssl http2;
    server_name api.domain.com;
    ...
}
```

- Inside that block, there’s a location directive
You’ll usually see something like:
```sh
location / {
    proxy_pass http://api_backend;
    proxy_set_header Host $host;
    ...
}
```

- nginx looks up that upstream
Earlier in the config, you defined:
```sh
upstream api_backend {
    server 10.0.0.5:8080;
    server 10.0.0.6:8080;
}
```

nginx picks one of the servers and sends the HTTP request there (e.g., to `http://10.0.1.5:8080/v1/users`)


The `proxy_pass` directive tells nginx where to send a request that matches a location block.
You can point it to either:
- A named upstream block (best practice for prod):
`proxy_pass http://api_backend;`
…where `api_backend` is defined earlier:
```conf
upstream api_backend {
    server 10.0.1.5:8080;
    server 10.0.1.6:8080;
}
```
- Or a direct URL or host:port (useful for simple one-off setups):

`proxy_pass http://127.0.0.1:8080;`
or
`proxy_pass http://internal-api:8080;`