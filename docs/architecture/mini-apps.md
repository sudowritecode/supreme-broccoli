# Signed Mini-App Foundation

## Scope

The platform now offers a **feature-gated, signed-manifest mini-app foundation**. It is a trust and launch-coordination layer, not a general-purpose plug-in runtime. It registers immutable signed manifests, verifies narrow declared permissions, issues caller-bound one-time launch tickets, and provides JavaScript/TypeScript and Python SDKs that reproduce the signature checks.

Mini apps are disabled by default. Production activation requires an external registration secret and a platform Ed25519 ticket-signing key pair. No fallback development key is embedded in application code or production configuration.

| Capability | Implemented behaviour |
|---|---|
| Registration | An operator presents `X-Mini-App-Registration-Key` and submits a manifest signed with its declared Ed25519 public key. |
| Identity | Manifests are immutable per `(appId, appVersion)` and define issuer, HTTPS origin, public key, signature, and declared scopes. |
| Permissions | Only `PROFILE_BASIC` and `CONTEXT_LAUNCH` are supported. Unknown scopes fail request deserialization. |
| Ticket issue | The authenticated account/device can request a ticket only by accepting exactly the registered scope set. |
| Ticket security | Tickets contain a random nonce, caller account/device binding, five-minute configurable expiry (maximum fifteen minutes), and a platform Ed25519 signature. |
| Consumption | A caller can consume only their own device-bound ticket, once, before expiry. |
| Browser SDK | The TypeScript SDK verifies manifest/ticket signatures with WebCrypto and dispatches only two allowlisted bridge methods. |
| Python SDK | The Python SDK verifies manifest/ticket signatures with Ed25519 and has no platform-action client. |

## Feature Configuration

```yaml
plinth:
  mini-apps:
    enabled: false
    registration-key: ${PLINTH_MINI_APPS_REGISTRATION_KEY:}
    ticket-private-key-base64: ${PLINTH_MINI_APPS_TICKET_PRIVATE_KEY_BASE64:}
    ticket-public-key-base64: ${PLINTH_MINI_APPS_TICKET_PUBLIC_KEY_BASE64:}
    ticket-lifetime-seconds: 300
```

The public key is X.509 SubjectPublicKeyInfo DER encoded as Base64. The ticket private key is PKCS#8 DER encoded as Base64. Configuration is intentionally invalid for ticket issuance until an operator supplies both halves of the platform key pair.

## API Contract

| Method and route | Authentication and purpose |
|---|---|
| `POST /api/v1/mini-apps/manifests` | Requires the operator registration-key header. Registers a valid Ed25519-signed manifest. |
| `GET /api/v1/mini-apps/{appId}/versions/{appVersion}/manifest` | Requires device authentication. Retrieves the immutable registered manifest. |
| `POST /api/v1/mini-apps/{appId}/versions/{appVersion}/launch-tickets` | Requires device authentication and exact declared-scope consent. Issues a short-lived signed ticket. |
| `POST /api/v1/mini-apps/launch-tickets/{ticketId}/consume` | Requires the same account and device. Consumes a live ticket exactly once. |

## Canonical Signing Payloads

All values are UTF-8 text in the shown line order. Each line, including the final line, ends with `\n`. Permission names are sorted lexicographically and joined with commas.

```text
appId={appId}
appVersion={appVersion}
issuer={issuer}
origin={origin}
permissions={sorted,comma,separated,permissions}
```

```text
ticketId={ticketId}
appId={appId}
appVersion={appVersion}
accountId={accountId}
deviceId={deviceId}
nonce={nonce}
expiresAt={unixEpochSecond}
permissions={sorted,comma,separated,permissions}
```

The manifest signature uses the manifest public key. The ticket signature uses the platform private key and is verified against the `platformPublicKeyBase64` sent with the ticket. The raw device token is never serialized into either payload.

## SDK Boundaries

| Package | Responsibility | Explicitly excluded |
|---|---|---|
| `sdks/javascript` | Browser-native WebCrypto verification and an allowlisted bridge facade. | Generic network methods, storage, raw device credentials, message/ciphertext access, contacts, payments, arbitrary host methods. |
| `sdks/python` | Server-side manifest/ticket verification using `cryptography` Ed25519 primitives. | Platform API client, app hosting, user data access, payment actions, runtime sandbox. |

> A successful signature verifies a data contract. It does **not** make third-party executable code trusted, reviewed, sandboxed, or eligible for privileged platform data.

## Deferred Production Work

Before enabling third-party mini apps in production, the platform needs operator authentication, developer onboarding, manifest review and suspension, public key rotation, durable/revocable per-scope consent, CSP/iframe/WebView sandboxing, origin attestation, mobile bridge isolation, rate limiting, structured audit logs, security scanning, abuse response, and a written policy for every additional scope. Financial capability, private-message access, device APIs, and arbitrary network privileges are all explicitly out of scope pending separate ADRs and security review.
