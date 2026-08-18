# Private-Text WebSocket Delivery Runbook

## Implemented Slice

The repository now provides a durable, authenticated private-text delivery path in addition to the existing encrypted-message ingress and RabbitMQ outbox relay.

```text
Authenticated sender HTTP request
  -> ciphertext-only message outbox record
  -> post-commit recipient delivery record
  -> authenticated recipient WebSocket delivery when connected
  -> recipient-scoped delivery acknowledgement
  -> durable DELIVERED state
```

The server continues to treat `ciphertext` as opaque client-produced transport data. It does not decrypt or inspect it.

## Endpoints

| Endpoint | Purpose | Authentication |
|---|---|---|
| `POST /api/v1/messages` | Accept an authorised opaque ciphertext envelope | `X-Device-Token` |
| `POST /api/v1/devices/{deviceId}/sessions` | Exchange an active compatible device token for a durable 30-day device session | `X-Device-Token`, and caller device must match `{deviceId}` |
| `DELETE /api/v1/devices/{deviceId}` | Revoke an account-owned device, its sessions, and its live delivery sockets | `X-Device-Token` |
| `GET /ws/v1/delivery` | Authenticated WebSocket endpoint for recipient-device ciphertext delivery | WebSocket handshake header `X-Device-Token` |

The initial registration response remains compatible with the prior development token flow. The new device-session record adds expiry, last-seen, and revocation semantics without breaking current clients. A later identity-hardening iteration must replace the temporary compatibility path with production device linking, recovery, session rotation policy, and audited mobile secure storage.

## WebSocket Protocol

The recipient device connects using the `X-Device-Token` header. The server retains device/account UUIDs only in the socket session attributes; it never retains the raw header value.

### Server event

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

Acknowledgements are accepted only from the authenticated recipient device and only when the supplied message ID matches the delivery record. Repeated acknowledgements are idempotent. A recipient reconnect receives its pending encrypted delivery again; the unique outbox/device constraint prevents a duplicate durable delivery record.

## Revocation Behaviour

Revoking a device marks the device as revoked, revokes all durable sessions for that device, and closes all matching live WebSocket sessions. After revocation, the legacy development token and any issued session token no longer authenticate to HTTP or WebSocket routes.

## Local Verification

```bash
./mvnw test
```

The integration suite covers an explicit device session, authenticated WebSocket connection, ciphertext delivery, disconnect/reconnect pending replay, idempotent acknowledgement, and WebSocket closure plus session invalidation on device revocation.

## Operational Limits and Next Work

| Implemented | Not implemented in this slice |
|---|---|
| Durable recipient delivery state | Mobile client and encrypted local message storage |
| In-process active WebSocket registry | Horizontally shared socket presence / broker-to-gateway fan-out |
| Durable device sessions | Production account recovery, device linking, session rotation UX |
| Reconnect replay | Content-free push notification provider adapter |
| Revocation closes local sockets | Cross-node socket invalidation and global rate limiting |
| Ciphertext-only transport boundary | Audited end-to-end encryption protocol integration and key transparency |

For multi-instance deployment, replace the in-process WebSocket registry with a gateway/presence abstraction backed by a shared transport and ensure broker delivery events are consumed idempotently by the appropriate gateway node. Do not add plaintext fallbacks to debug delivery failures.
