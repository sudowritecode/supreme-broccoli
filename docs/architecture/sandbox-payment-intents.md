# Sandbox Payment Intents

## Scope

The platform now provides a **provider-neutral, sandbox-only payment-intent interface**. It coordinates a mock authorisation lifecycle for development and future partner-integration preparation. It does not move money, process a card or bank credential, connect an account, create a wallet, hold a balance, settle a transfer, pay out funds, execute a refund, record accounting entries, or represent a financial transaction as final.

| Capability | Implemented behaviour |
|---|---|
| Feature gate | Payment APIs are disabled by default via `plinth.payments.enabled`. |
| Amount model | Each intent accepts a positive integer `amountMinor` and uppercase three-letter `currency`; floating point amounts are absent. |
| Idempotency | Creation requires `X-Idempotency-Key`. Repeating the same owner/key/amount/currency returns the original intent; conflicting reuse fails. |
| Lifecycle | Sandbox intents begin at `REQUIRES_AUTHORIZATION` and may become `AUTHORIZED` or `CANCELLED`. Neither terminal state can transition again. |
| Ownership | An intent can be read, authorized, or cancelled only by its creating account. |
| Provider boundary | `PaymentProviderPort` is the future adapter seam. `SandboxPaymentProvider` emits deterministic local references only and makes no external call. |
| Live capability | The base configuration hardcodes `live-enabled: false`, and the API refuses a live-activation attempt. No live provider exists. |
| Isolation | No payment route accepts payment credentials, bank data, recipient/payout details, message data, mini-app ticket, or app-provided payment permission. |

## Configuration

```yaml
plinth:
  payments:
    enabled: false
    live-enabled: false
```

The `live-enabled` setting is deliberately not exposed as a normal environment-variable switch in this release. If an operator overrides it through Spring configuration, the service refuses all payment operations instead of treating it as permission to execute a live transaction.

## API Contract

All routes require `X-Device-Token` authentication. Creation also requires `X-Idempotency-Key`.

| Method and route | Purpose |
|---|---|
| `POST /api/v1/payments/intents` | Create or idempotently replay a sandbox intent. Body contains only `amountMinor` and `currency`. |
| `GET /api/v1/payments/intents/{paymentIntentId}` | Retrieve the authenticated owner's sandbox intent. |
| `POST /api/v1/payments/intents/{paymentIntentId}/authorize` | Perform a simulated authorization transition. |
| `POST /api/v1/payments/intents/{paymentIntentId}/cancel` | Cancel a pending sandbox intent. |
| `POST /api/v1/payments/live/activation` | Always refused. It is an explicit safeguard test surface, not an activation mechanism. |

Every response includes `sandbox: true`. Clients must not display an `AUTHORIZED` sandbox response as a real payment success or settlement outcome.

## State Model

```text
Create with idempotency key
  -> REQUIRES_AUTHORIZATION
  -> authorize -> AUTHORIZED (terminal)
  -> cancel    -> CANCELLED  (terminal)

Reuse same idempotency key + same amount/currency -> original intent
Reuse same idempotency key + changed amount/currency -> rejected
Any live activation request -> rejected
```

## Provider Boundary

`PaymentProviderPort` currently has only one responsibility: producing a simulated provider reference for a sandbox intent. Future provider adapters must be implemented in a separately reviewed module. Provider credentials, webhooks, personal financial data, reconciliation events, and provider-specific response fields must not be stored in `payment_intents` without a new data classification and compliance review.

> A sandbox payment intent is an integration-development artifact. It is **not a charge, transfer, authorization hold, receipt, settlement, stored-value balance, or financial ledger entry**.

## Data Model

| Table | Purpose |
|---|---|
| `payment_intents` | Owner-bound idempotency key, minor amount, currency, sandbox lifecycle, provider reference, and timestamps. |

No tables for accounts at financial institutions, card data, balances, beneficiaries, payouts, ledger journals, refunds, provider events, or KYC data are present.

## Conditions Before Live Partner Work

Before any live payment capability, the platform must select a regulated partner and complete partner due diligence, jurisdictional/legal review, security architecture, PCI/PII classification, consent and disclosure design, fraud/KYC/AML/sanctions controls as applicable, encrypted signed webhook validation, replay defense, reconciliation, double-entry ledger design, financial reporting, disputes/refunds/chargebacks, payout controls, data-retention policy, audit logging, observability, incident response, penetration testing, mobile confirmation UX, and production operational readiness. Each irreversible financial capability requires a separate ADR and implementation review.
