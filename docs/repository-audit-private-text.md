# Repository Audit — Private-Text Delivery Slice

**Audit date:** 18 August 2026  
**Scope:** Read-only onboarding review completed before adapting the private-text MVP backend.

## Repository Fact Sheet

| Area | Finding |
|---|---|
| Branch state | `main` is clean and tracks `origin/main`. |
| Runtime/build | Java 21, Maven Wrapper, Spring Boot 3.4.4. |
| Backend style | Modular Spring Boot monolith with REST controllers, JPA, Flyway, RabbitMQ AMQP, and Actuator. |
| Local data profile | H2 is the default local/test datastore; PostgreSQL profile/configuration and Compose service exist for persistent development. |
| Eventing | RabbitMQ topology is configured only when the `broker` profile is enabled. The project uses an encrypted-message transactional outbox with retry. |
| Existing schema | V1 covers accounts, devices, contact requests, direct conversations, and conversation membership. V2 covers ciphertext-only message outbox records. |
| Identity | A device is registered with a public identity key and a SHA-256 hash of a development bearer token. Revocation state exists on the device record. |
| Current messaging | Authenticated API ingress stores an opaque ciphertext envelope; it authorises active sender and recipient conversation membership and applies message-ID idempotency. |
| Delivery gap | No per-device durable delivery state, WebSocket endpoint, WebSocket handshake authentication, delivery worker, delivery receipt, or content-free push wake-up exists. |
| Client gap | No mobile application or client encryption-library integration is present. |
| Security boundary | The existing service does not accept plaintext message content and does not log ciphertext in the examined delivery path. The development bearer token is explicitly not production authentication. |

## Current Data Flow

```text
Client request with X-Device-Token
  -> DeviceAuthenticator hashes token and resolves active device
  -> MessageIngressService validates direct-conversation membership
  -> message_outbox stores opaque ciphertext and idempotency key
  -> after commit, broker-enabled relay publishes encrypted event
  -> RabbitMQ outbound queue

Current missing path:
  RabbitMQ/outbox -> per-device delivery state -> authenticated WebSocket -> client acknowledgement -> delivery receipt
```

## Adaptation Decision

The project is suitable for incremental adaptation. The next slice will preserve the existing ciphertext envelope, direct-conversation authorisation, outbox, H2/PostgreSQL profiles, and RabbitMQ topology. It will add a **versioned Flyway migration** and narrowly scoped modules rather than replacing the architecture.

| Change | Adaptation approach |
|---|---|
| Session boundary | Retain current development device-token compatibility for the existing registration flow, but introduce an explicit durable device-session model with expiry and revocation semantics. |
| Delivery state | Add one durable delivery record per outbox item and recipient device, with idempotent pending/delivered states and timestamps. |
| WebSocket | Add Spring WebSocket support with a handshake interceptor that authenticates `X-Device-Token` and attaches only device/account identifiers to the session. |
| Delivery worker | Listen after transaction commit and during retry sweeps; create/check durable delivery records before pushing the opaque envelope to an active recipient socket. |
| Acknowledgement | Add authenticated client acknowledgement that marks only the caller's matching device delivery record as delivered. |
| Revocation | Expose authenticated device revocation and close active WebSocket sessions for revoked devices. |
| Push wake-up | Keep as a future adapter boundary; no provider or message-content push payload will be introduced in this slice. |

## Migration Plan

The implementation will add a V3 migration for `device_sessions` and `message_deliveries`. Existing `devices.access_token_hash` remains intact for backward compatibility during this MVP iteration. A later identity-hardening loop can retire the development token column only after mobile session/device linking and recovery flows exist.

## Immediate Security Constraints

1. The WebSocket handshake must not accept a caller-selected sender/device identifier as proof of identity.
2. No WebSocket payload, API response, log entry, or error message may contain plaintext message content, private keys, raw bearer tokens, or raw payment credentials.
3. A revoked device must be unable to authenticate, receive a delivery, acknowledge a delivery, or hold an active WebSocket connection.
4. Delivery transitions must be idempotent. Repeated broker events, reconnects, and acknowledgements must not create duplicate delivery state or duplicate receipts.
5. WebSocket delivery does not establish production end-to-end encryption. It transports only the client-encrypted ciphertext envelope already accepted by the platform.

## Baseline Validation

The repository's pre-adaptation build and tests previously passed. The adaptation will preserve this baseline and add specific tests for WebSocket handshake authentication, delivery creation, reconnect, acknowledgement idempotency, and revocation enforcement.
