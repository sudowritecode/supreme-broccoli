# Signal Protocol Integration Foundation

## Scope and Status

Plinth now has a **feature-gated, public-key-directory and opaque-envelope foundation** for the `SIGNAL_PQXDH_DOUBLE_RATCHET_V1` client profile. It adds the server contracts required by an audited client integration; it does not move Signal cryptography into the backend.

> The backend does **not** currently make Plinth Signal Messenger-compatible, independently audited, cryptographically complete, or production-ready end-to-end encrypted. It prepares the public-bundle, authorization, and transport surfaces that a reviewed client implementation needs.

The selected profile follows the current Signal design direction: an asynchronous PQXDH-style public bundle establishes the initial shared-secret context, then the Double Ratchet advances message keys client-side. The Double Ratchet specification describes a root chain plus sending/receiving chains that derive per-message keys and mix fresh Diffie-Hellman output for forward-security and break-in-recovery properties. [1] The PQXDH specification adds signed Kyber public prekeys to the identity key, signed EC prekey, and optional EC one-time prekey material. [2]

| Capability | Status in this repository |
|---|---|
| Public device identity and signed EC prekey directory | Implemented |
| Signed Kyber last-resort and one-time public prekey directory | Implemented |
| Atomic one-time EC/Kyber allocation | Implemented |
| Contact- or self-account-gated bundle discovery | Implemented |
| Client-side PQXDH library binding | Implemented as reviewed-library adapter scaffold against `@signalapp/libsignal-client` declarations |
| Client-side Double Ratchet encryption/decryption calls | Implemented in the adapter scaffold; secure stores must be supplied by each client |
| Opaque prekey/ratchet envelope ingress | Implemented |
| Safety-number state | Client-calculated, client-submitted verification state implemented |
| Sender-key group encryption | Deliberately deferred and rejected at the pairwise endpoint |
| Private-key/session-key/plaintext server handling | Explicitly prohibited and absent |

## Trust Boundary

The server handles durable authorization and public material. The client handles all secret cryptographic state.

| Server stores or processes | Client alone stores or processes |
|---|---|
| Public identity key, signed EC prekey, EC signature, signed Kyber public prekeys/signatures, key identifiers, expiry metadata, one-time claim metadata, encrypted envelope text, conversation/device routing metadata, and client-recorded verification state | Identity private key, signed-prekey private key, EC one-time private key, Kyber secret key, initiator ephemeral private key, PQXDH secrets, Double Ratchet root/chain/message keys, skipped keys, ratchet state, sender keys, group plaintext, message plaintext, and safety-number derivation inputs |

The server never receives or derives a private key, shared secret, ratchet key, plaintext, or group sender key. It never verifies or calculates a safety number. It does not parse the serialized Signal ciphertext beyond its version/profile and kind framing.

## Public Bundle Lifecycle

A client uses `POST /api/v1/signal/device-bundle` to publish public material for its authenticated device. The request contains a profile, numeric protocol-device and registration identifiers, public identity key, signed EC prekey and signature, signed Kyber last-resort prekey, signed Kyber one-time prekeys, and EC one-time prekeys. The request accepts no private material.

Clients retrieve only authorized devices through `GET /api/v1/signal/accounts/{username}/devices`. The same account may access its own device list; another account must be an accepted contact. A client claims an individual device bundle through `POST /api/v1/signal/devices/{deviceId}/prekey-bundle:claim`.

| Bundle component | Server behaviour |
|---|---|
| Identity key | Stores only the client-provided public encoding. An update marks prior peer-device verification records as `CHANGED`. |
| Signed EC prekey | Stores public key, identifier, signature, and expiry. The client validates the signature before session initialization. |
| EC one-time prekey | Allocates the oldest available public key under a pessimistic lock and marks it claimed once. It may be absent when inventory is exhausted. |
| Signed Kyber one-time prekey | Allocates the oldest available public key under a pessimistic lock and marks it claimed once. |
| Signed Kyber last-resort prekey | Returns the latest public key only when no Kyber one-time prekey is available. It is not consumed. |

The protocol specifications require clients to reject an invalid signed bundle and establish a session only after validating the relevant signatures. [2] The server's atomic allocation stops the same one-time public record from being handed out repeatedly; it cannot prove that a client removed the corresponding private key. Secure private-key lifecycle remains a client responsibility.

## Client Adapter and Secure Stores

`sdks/signal-typescript` includes `LibsignalPqxdhSessionBootstrapper`, a typed binding to the current official `@signalapp/libsignal-client` declarations. It validates EC and Kyber signatures, builds a `PreKeyBundle`, calls `processPreKeyBundle`, encrypts through `signalEncrypt`, decrypts prekey or ratchet envelopes through the matching library function, and calculates a displayable `Fingerprint`.

The adapter requires application-provided `SessionStore`, `IdentityKeyStore`, `PreKeyStore`, `SignedPreKeyStore`, and `KyberPreKeyStore` implementations. They must use platform secure storage and atomically persist every ratchet-state transition. The server API, browser local storage, plain browser IndexedDB, in-memory test stores, and unencrypted desktop files are not acceptable production stores.

The official libsignal repository exposes Java, Swift, and TypeScript APIs backed by Rust, but states that external use is unsupported, APIs can change, and the source/package license is AGPL-3.0. A legal, security, distribution, and maintenance review is mandatory before shipping it in a product client. [3]

## Opaque Envelope Transport

Clients serialize a reviewed-library ciphertext as Base64 and wrap it as:

```text
SIGNAL_PQXDH_DOUBLE_RATCHET_V1.PREKEY.<opaque-serialized-ciphertext-base64>
SIGNAL_PQXDH_DOUBLE_RATCHET_V1.RATCHET.<opaque-serialized-ciphertext-base64>
```

`POST /api/v1/signal/messages` validates the profile/kind framing and delegates sender authentication, active conversation membership, recipient-device authorization, idempotency, outbox persistence, and authenticated delivery to the existing message ingress path. The server does not deserialize or decrypt the final ciphertext segment.

The encrypted **client application payload** must itself include `conversationId`, sender device, recipient device, and message ID. The recipient must compare these decrypted values with the expected routing context before displaying content. This prevents the client from treating server routing metadata as a sufficient cryptographic context-binding mechanism.

## Identity Verification

A client derives a safety number/fingerprint locally from the relevant identity public keys and stable identifiers, displays it for authenticated out-of-band comparison, and only then posts its own verification decision to `POST /api/v1/signal/devices/{deviceId}/identity-verification`. The stored state is scoped from one verifier device to one subject device and is labeled `UNVERIFIED`, `VERIFIED`, or `CHANGED`.

The X3DH/PQXDH specifications state that without an authenticated identity comparison, parties have no cryptographic guarantee about whom they are communicating with. [2] The `VERIFIED` state is therefore a **client claim**, not a server attestation. When a target device changes its registered identity key, every stored verification record targeting that device becomes `CHANGED`; clients must block or require explicit re-verification before sending sensitive content.

## Multi-Device and Group Boundaries

Each recipient device has an individual public bundle and individual pairwise PQXDH/Double Ratchet session. A sender establishes sessions with each active recipient device and sends one authorized opaque envelope per recipient device. Device additions, removals, revocations, and identity-key changes require client session refresh and explicit user-facing trust handling.

`GROUP` envelopes are rejected at the current pairwise Signal endpoint. The deferred `PLINTH_SIGNAL_GROUP_SENDER_KEYS_V1` profile will use pairwise sessions to distribute sender-key material, bind group payloads to server membership versions, rekey after membership/device changes, and fan out opaque envelopes to active recipient devices. ADR-0010 records the boundary in detail.

## Security Gates Before Feature Activation

| Gate | Required outcome |
|---|---|
| Dependency assessment | Legal review of AGPL and distribution obligations; pinned library version, SBOM, vulnerability monitoring, and upgrade owner. |
| Client secure storage | OS-backed key storage, encrypted durable ratchet-store implementation, crash consistency, key deletion, and device-transfer/recovery policy. |
| Protocol interoperability | Test vectors and cross-platform interoperability for all target Android, iOS, desktop, and web runtimes. |
| Identity UX | Safety-number/QR comparison, key-change blocks, re-verification flow, device-management UI, and recovery escalation. |
| Attachments and notifications | Separate attachment encryption, metadata minimization, push-notification privacy, and media-preview policy. |
| Group profile | Sender-key distribution, rekey behavior, membership-version binding, multi-device fan-out, and group interop tests. |
| Independent assessment | Cryptographic implementation review, threat model, penetration test, privacy review, incident response, and staged rollout. |

## References

[1]: https://signal.org/docs/specifications/doubleratchet/ "The Double Ratchet Algorithm"

[2]: https://signal.org/docs/specifications/pqxdh/ "The PQXDH Key Agreement Protocol"

[3]: https://github.com/signalapp/libsignal "signalapp/libsignal"
