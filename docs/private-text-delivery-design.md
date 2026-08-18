# Private-Text Delivery Design

## Scope

This design extends the existing authenticated ciphertext ingress and transactional outbox with durable recipient-device delivery state, authenticated WebSocket transport, acknowledgement handling, and revoked-device enforcement. It does not add a mobile client, implement a production encryption protocol, or introduce message plaintext into the backend.

## Delivery Flow

```text
Sender device
  -> authenticated HTTP ciphertext ingress
  -> message_outbox (existing durable source record)
  -> message_deliveries (recipient device state: PENDING)
  -> authenticated WebSocket session for recipient device
  -> opaque encrypted-message delivery envelope
  -> authenticated delivery acknowledgement
  -> message_deliveries status: DELIVERED
  -> future receipt event / content-free push adapter
```

## V3 Schema

| Table | Purpose | Important constraints |
|---|---|---|
| `device_sessions` | Explicit device-session verifier with expiry/revocation metadata | Token hash is unique; a session belongs to one device; raw token is never stored |
| `message_deliveries` | Per-recipient-device delivery state for every encrypted outbox record | Unique `(outbox_id, recipient_device_id)` prevents duplicate deliveries; only `PENDING` and `DELIVERED` states are valid |

The existing `devices.access_token_hash` stays in place during this iteration to avoid breaking the registration flow. `DeviceAuthenticator` will resolve either an active durable session or the compatible development device token; all successful authentication returns the same device/account principal. A future device-trust iteration will retire the legacy column after mobile session and recovery functionality exists.

## WebSocket Endpoint

| Item | Contract |
|---|---|
| Endpoint | `/ws/v1/delivery` |
| Handshake authentication | `X-Device-Token` header; server resolves an active device principal and stores only device/account UUIDs as WebSocket session attributes |
| Transport | Text JSON over authenticated WebSocket; server-to-client delivery only in this iteration |
| Server message | `encrypted_message` event with delivery ID, message ID, conversation ID, sender device ID, ciphertext, received timestamp |
| Client message | `delivery_ack` event containing delivery ID and message ID |
| Forbidden payloads | Plaintext messages, private keys, device tokens, arbitrary sender identities, payment credentials, arbitrary mini-app content |
| Revocation behavior | Server closes all matching device WebSocket sessions; subsequent handshake and acknowledgement fail authentication |

### Server delivery envelope

```json
{
  "type": "encrypted_message",
  "deliveryId": "uuid",
  "messageId": "uuid",
  "conversationId": "uuid",
  "senderDeviceId": "uuid",
  "ciphertext": "opaque client-encrypted data",
  "receivedAt": "ISO-8601 instant"
}
```

### Client acknowledgement

```json
{
  "type": "delivery_ack",
  "deliveryId": "uuid",
  "messageId": "uuid"
}
```

An acknowledgement is idempotent and is accepted only when the WebSocket's authenticated device is the delivery record's recipient device and the record's message ID matches. Unknown, spoofed, malformed, or revoked-session acknowledgements do not mutate delivery state.

## Components

| Component | Responsibility |
|---|---|
| `DeviceSessionService` | Issue/revoke/resolve explicit device-session tokens while retaining compatibility lookup for the existing development token |
| `DeviceAuthenticator` | Use active session or legacy active token to return an authenticated device principal |
| `MessageDelivery` / repository | Create, query, and transition durable per-device delivery records idempotently |
| `DeliveryWebSocketHandshakeInterceptor` | Authenticate the handshake and place non-sensitive principal identifiers into session attributes |
| `DeliveryWebSocketRegistry` | Register/unregister live sessions and close sessions for a revoked device |
| `DeliveryWebSocketHandler` | Parse acknowledgement messages and invoke delivery acknowledgement service; never process plaintext message content |
| `MessageDeliveryService` | Create pending delivery state after encrypted message ingress and deliver pending records to active recipient sockets |
| `MessageOutboxRelay` | Retain broker publishing behavior; delivery remains independently durable and retryable |
| `DeviceRevocationService` | Revoke device and active sessions, then close the matching WebSocket sessions |

## State Transitions

| Entity | State transition | Guard |
|---|---|---|
| Device session | ACTIVE → REVOKED / EXPIRED | Session belongs to device; timestamps are authoritative |
| Device | ACTIVE → REVOKED | Caller owns device or future account-recovery policy authorises action |
| Message delivery | PENDING → DELIVERED | Authenticated recipient device matches; acknowledged message ID matches |
| WebSocket session | CONNECTED → CLOSED | Device revocation, normal disconnect, invalid handshake, or server shutdown |

## Failure and Retry Semantics

1. The delivery record is durable before a WebSocket send attempt.
2. A disconnected recipient remains `PENDING`; reconnect triggers a pending-delivery replay.
3. A repeated outbox event or reconnect does not create a second delivery record.
4. A repeated acknowledgement does not change a delivered state or produce a duplicate receipt.
5. A WebSocket send failure leaves the record `PENDING` and removes/cleans the failed live session.
6. The future content-free push adapter will be invoked from pending delivery state, not from plaintext content.

## Security Tests Required

- Handshake fails with no token, invalid token, expired session, and revoked device.
- A live WebSocket session closes on device revocation.
- A sender cannot acknowledge the recipient's delivery.
- A message ID mismatch cannot mark a delivery as delivered.
- Multiple delivery attempts preserve a single delivery record.
- Reconnect replays only pending ciphertext envelopes for the authenticated recipient device.
- No log fixture or wire contract includes plaintext or raw token values.
