# Plinth Signal Client Contract SDK

This package provides the client-side integration boundary for Plinth’s `SIGNAL_PQXDH_DOUBLE_RATCHET_V1` profile. The server exposes public bundles, allocates one-time public prekeys, routes opaque envelopes, and records a client-submitted verification state. It does **not** establish sessions, verify cryptographic signatures, ratchet messages, decrypt messages, calculate safety numbers, or retain private keys.

The `LibsignalPqxdhSessionBootstrapper` binds to the current documented APIs of [`@signalapp/libsignal-client`](https://github.com/signalapp/libsignal). It uses the package’s `PreKeyBundle`, `processPreKeyBundle`, `signalEncrypt`, `signalDecrypt`, `signalDecryptPreKey`, and `Fingerprint` interfaces. No PQXDH, Double Ratchet, AEAD, KDF, signature, or sender-key algorithm is reimplemented in this SDK.

> The official `libsignal` repository states that third-party use is unsupported and APIs may change. Its published package is AGPL-3.0-only. Product counsel, dependency-maintenance owners, security review, and target-platform distribution review must approve its use before it is included in a shipped client.

## Installation and Verification

```bash
pnpm install --ignore-scripts
pnpm check
```

The optional peer dependency is installed only to type-check the adapter against the current official declarations. Do not run an unreviewed native cryptographic package in a production browser context merely because the SDK type-checks.

## Required Client Responsibilities

| Responsibility | Required behaviour |
|---|---|
| Secure state | Implement the libsignal `SessionStore`, `IdentityKeyStore`, `PreKeyStore`, `SignedPreKeyStore`, and `KyberPreKeyStore` using OS-backed secure storage and encrypted durable state. |
| Prekey generation | Generate identity, signed EC, one-time EC, signed Kyber last-resort, and signed one-time Kyber private keys locally. Upload only corresponding public bundle material. |
| Signed bundle validation | Reject a server bundle when either the EC signed-prekey or Kyber signed-prekey signature fails against the peer identity key. |
| One-time key deletion | Remove a locally consumed EC one-time prekey after successful prekey-message processing; follow the library’s Kyber used-key lifecycle. |
| Ratchet persistence | Persist every session-state update atomically before acknowledging a received message. Do not roll ratchet state backward after a crash. |
| Payload binding | Encrypt a structured client payload containing `conversationId`, sender device, recipient device, and message ID. After decrypting, compare it to the expected routing context before displaying content. |
| Safety numbers | Calculate the fingerprint locally from the identity keys and stable identifiers. Require users to compare the displayed code or QR data over an authenticated channel before recording `VERIFIED`. |
| Identity changes | Treat a server response of `CHANGED` as a blocking identity change until the user explicitly re-verifies the new fingerprint. |

## Session Bootstrap Sequence

1. Authenticate to Plinth and retrieve an authorized peer-device list.
2. Claim one PQXDH bundle for each target device. The server may supply a one-time EC/Kyber key or a Kyber last-resort key when one-time inventory is depleted.
3. Verify both signed public prekeys with the peer’s public identity key.
4. Convert the validated bundle into the official libsignal `PreKeyBundle` and call `processPreKeyBundle`.
5. Persist the resulting client session through the secure stores.
6. Serialize the resulting prekey or ratchet ciphertext as Base64 and wrap it as `SIGNAL_PQXDH_DOUBLE_RATCHET_V1.PREKEY.<body>` or `SIGNAL_PQXDH_DOUBLE_RATCHET_V1.RATCHET.<body>`.
7. Send the opaque envelope through `POST /api/v1/signal/messages` with existing conversation/device routing metadata.

## Group Messages

`GROUP` envelopes are intentionally rejected by the current signal-message endpoint. The current SDK does not promote a pairwise PQXDH/Double Ratchet session into a group protocol. Group sender-key distribution, membership-version binding, rekey-on-membership-change, recipient-device fan-out, and authenticated encrypted group payload handling are separate work and must use a separately versioned profile.

## Non-Claims

This package does not make Plinth Signal Messenger-compatible, production secure, interoperable with Signal’s service, or independently audited. A production client additionally needs key backup/recovery policy, registration/device transfer policy, attachment encryption, push-notification privacy design, key-change UX, multi-device synchronization, test vectors, interop tests, external cryptographic review, penetration testing, privacy review, and incident processes.
