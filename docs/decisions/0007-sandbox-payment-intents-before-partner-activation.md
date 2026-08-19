# ADR-0007: Use Sandbox Payment Intents Before Partner Activation

**Status:** Accepted  
**Date:** 19 August 2026  
**Decision owner:** Manus AI, per user instruction to proceed autonomously and record decisions for review.

## Context

The product must be ready for a future regulated financial partner, but no bank, acquirer, payment institution, stored-value issuer, or processor partner has been selected. The platform therefore has no authority to move money, hold funds, issue payment credentials, settle transactions, accept regulated personal financial data, or represent a completed payment as financially final.

The existing product has private communications, rooms, games, and mini-app contracts. Any financial interface must remain isolated from message ciphertext, device tokens, and third-party mini-app permissions. It must not create an implied live-payment path that could accidentally be activated without legal, compliance, risk, reconciliation, fraud, security, and partner controls.

## Decision

Implement a **provider-neutral sandbox payment-intent API**. An authenticated account may create an idempotent sandbox intent using an explicit client idempotency key, a minor-unit amount, and a three-letter currency. The intent supports controlled mock authorization, cancellation, and readback states. It is not a charge, transfer, wallet, ledger entry, stored-value balance, bank connection, or settlement record.

A `PaymentProviderPort` defines the future adapter boundary. The only supplied adapter is `SandboxPaymentProvider`, which creates deterministic simulated provider references and performs no external request. Live providers remain permanently refused in code unless a separately shipped partner activation module explicitly adds a regulated provider, non-sandbox credentials, compliance controls, and a security-reviewed feature gate. The baseline configuration sets `live-enabled: false`; there is deliberately no supported environment variable that turns live payment execution on in this release.

## Consequences

| Decision | Consequence |
|---|---|
| Minor-unit amounts only | Avoids float rounding and forces a specific currency representation. |
| Request idempotency | Safe caller retries resolve to the original intent and reject conflicting payload reuse. |
| Sandbox-only adapter | No payment data leaves the application and no external money movement occurs. |
| Separate provider port | Future integrations do not contaminate generic intent records with provider-specific credentials. |
| Live execution hard refusal | No configuration change in this slice can cause a real payment. |
| No balance/ledger | There is no claim of stored funds, accounting finality, or reconciled settlement. |
| No mini-app payment scope | A signed mini-app cannot invoke the payment API through the current bridge. |

## Acceptance Criteria

1. An authenticated account can create an idempotent sandbox payment intent in minor units with a valid ISO-like three-letter currency code.
2. Reusing the same idempotency key with the same payload returns the original intent; a changed amount/currency is rejected.
3. Only the intent owner can view, authorize, or cancel it.
4. Authorization and cancellation follow explicit state transitions; an authorized/cancelled intent cannot be replayed into another terminal state.
5. Every response states `sandbox: true`; no endpoint accepts payment credential, bank account, card, wallet, payout, or recipient fields.
6. Any live-provider activation request is refused because the base platform does not include a regulated partner implementation.
7. Payment records remain distinct from message, conversation, room, game, mini-app ticket, and financial balance data.

## Deferred Work

A regulated integration milestone must add a selected licensed partner, jurisdictional scope, legal/compliance approval, PCI/PII boundary design, customer due diligence/KYC as applicable, sanctions/AML and fraud controls, encrypted provider webhooks, reconciliation, double-entry accounting design, dispute/refund handling, payout controls, currency/FX policy, observability, incident response, third-party risk assessment, penetration testing, mobile confirmation UX, and a separate ADR for each irrevocable financial capability.
