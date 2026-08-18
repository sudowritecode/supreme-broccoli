# Messaging MVP Backend

## Implemented vertical slice

The backend now supports the minimum server-side chain needed to accept a private-message envelope safely: a user registers a username and initial device, exchanges a consent-based contact request, accepts that request to create a direct conversation, and then submits an opaque ciphertext envelope using a device token. The server derives sender identity from that token; it does not trust a caller-supplied sender device identifier.

```text
Client device
  ├─ register account + public identity key
  │    └─ receives one-time development device token
  ├─ contact request by username
  │    └─ recipient accepts
  │         └─ direct conversation + two memberships
  └─ POST encrypted envelope with X-Device-Token
       ├─ authenticate active sender device
       ├─ authorize sender and recipient as conversation members
       ├─ persist ciphertext-only message outbox record
       └─ after commit, publish to RabbitMQ when broker profile is active
```

The message-ingress transaction persists the encrypted envelope before it creates a broker-publication event. When the broker profile is active, an after-commit relay publishes the message and marks the outbox item as `PUBLISHED`; a scheduled sweep retries remaining `PENDING` records. This provides **at-least-once** broker publication, so any future delivery worker must treat `messageId` as its idempotency key.

## Persisted data boundary

| Record | Stored fields | Explicitly excluded |
|---|---|---|
| Account | UUID, normalized username, creation/disable timestamps | Passwords, address book, location, profile graph |
| Device | Owning account, public identity key, label, token hash, revocation state | Raw device token, private key, pre-key private material |
| Contact request | Sender, recipient, consent state, timestamps | Message content and social-discovery metadata |
| Conversation | Canonical direct-participant key and active memberships | Readable conversation content |
| Message outbox | Message/conversation/device identifiers, opaque ciphertext, attempt and publish state | Plaintext content, attachment plaintext, decryption keys |

> The `ciphertext` value is transport data only. It must be encrypted before it reaches this API, and no controller, service, log statement, error handler, or broker payload may introduce a plaintext alternative.

## Local profiles

| Runtime profile | Database | Broker behavior | Intended use |
|---|---|---|---|
| Default | Ephemeral H2 | Disabled; encrypted outbox items remain `PENDING` | Fast local API work and automated tests |
| `postgres` | PostgreSQL | Disabled unless `broker` is also active | Durable local development |
| `broker` | Current configured database | RabbitMQ topology and post-commit relay enabled | Broker integration testing |
| `postgres,broker` | PostgreSQL | RabbitMQ topology, durable outbox relay, retry sweep | Full local MVP dependency stack |

## Security and operational constraints

The device token is deliberately described as a **development authentication mechanism**. It is a randomly generated bearer token returned only at initial registration and stored server-side only as a SHA-256 hash. It is not a substitute for production device registration, user recovery, device linking, key transparency, session management, key rotation, rate limiting, abuse prevention, or audited end-to-end encryption.

| Control already enforced | Implementation |
|---|---|
| Caller identity cannot select a sender device | The authenticated `X-Device-Token` resolves the sender device and account. |
| Revoked or unknown devices cannot authenticate | Token hash lookup requires an active, non-revoked device. |
| A direct chat requires mutual consent | Direct conversation creation occurs on contact-request acceptance. |
| A sender cannot target an unrelated device | Both sender and recipient accounts must be active members of the supplied conversation. |
| Network retries do not duplicate stored messages | The caller-provided `messageId` is unique; a byte-for-byte matching retry returns `duplicate`. |
| Broker failures do not erase accepted messages | The message is stored as `PENDING` and retryable before broker publication. |

## Next MVP backend loop

The next implementation loop should add a delivery worker that consumes the encrypted broker event, uses `messageId` for idempotency, stores only the allowed encrypted delivery envelope and delivery state, and emits a content-free push wake-up. It should be preceded by explicit account/device rate limits, a device-revocation endpoint, retention settings for delivery and dead-letter records, and an audit-safe abuse-report model.
