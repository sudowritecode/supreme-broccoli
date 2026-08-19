# ADR-0009: Adopt a Client-Side Signal-Protocol Integration Foundation

**Status:** Accepted  
**Date:** 19 August 2026  
**Decision owner:** Manus AI, per the user’s instruction to proceed autonomously and record decisions for review.

## Context

The existing service provides ciphertext-only transport, authenticated device sessions, active conversation membership checks, durable delivery state, and private group/room controls. These controls do not by themselves establish end-to-end encryption: a complete secure-messaging implementation requires authenticated asynchronous session setup, per-message ratcheting, device identity verification, multi-device routing, and a client-side group-encryption protocol.

The official Signal specifications assign different roles to clients and servers. X3DH requires recipients to publish an identity key, a signed prekey, its signature, and a stock of optional one-time prekeys; the server should deliver a bundle and atomically remove an allocated one-time prekey. The Double Ratchet uses evolving private session state and per-message keys, which must remain client-side. Identity comparison through an authenticated channel is necessary for a cryptographic guarantee of peer identity. [1] [2]

The official `libsignal` project supplies Java, Swift, and TypeScript wrappers, but its README states that use outside Signal is unsupported, APIs may change, and the project is AGPL-3.0 licensed. It cannot be adopted into a distributed product without license, maintenance, target-platform, and independent-security review. [3]

## Decision

Implement a **protocol-versioned server integration foundation** for `SIGNAL_PQXDH_DOUBLE_RATCHET_V1`. This label describes the target profile and server contract; it does not assert interoperability with Signal Messenger or claim that a client has completed a cryptographic session.

The server will store and route **public** identity material, signed EC-prekey metadata/signatures, signed Kyber last-resort/one-time prekey metadata/signatures, and EC one-time prekey public values. It will atomically allocate a one-time prekey when a bundle is fetched, falling back to the latest signed Kyber last-resort prekey only when Kyber one-time inventory is empty. It will accept only opaque, client-produced protocol envelopes for existing conversation delivery and will bind those envelopes to the existing authenticated device and active-membership controls.

The server will never receive, generate, derive, decrypt, retain, back up, or log private identity keys, signed-prekey private keys, one-time-prekey private keys, ephemeral private keys, X3DH/PQXDH shared secrets, Double Ratchet root/chain/message keys, skipped-message keys, group sender keys, plaintext, or safety-number secrets.

Client integrations are scaffolded as an explicit official-library adapter, secure-store interfaces, and package metadata. No homemade PQXDH, Double Ratchet, AEAD, KDF, signature, or group-key implementation is written in TypeScript, Python, or Java. A shipping client must select an actively maintained, legally reviewed, independently evaluated library per target platform, then implement secure platform storage, signature validation, session persistence, key deletion, recovery, identity-change UX, and authenticated safety-number verification.

## Consequences

| Decision | Consequence |
|---|---|
| Public-bundle server contract | Enables asynchronous client session creation without exposing private key material. |
| Atomic one-time prekey allocation | Prevents one published prekey from being handed to multiple session initiators. |
| Client-only ratchet state | Preserves the server ciphertext-only trust boundary and limits compromise blast radius. |
| Versioned opaque envelopes | Allows clients to distinguish protocol payloads without server interpretation or decryption. |
| Client verification state only | Lets devices record a user decision while avoiding a false server attestation of identity. |
| Deferred libsignal binding | Avoids silently accepting unsupported API, AGPL, binary-distribution, or platform-maintenance risk. |
| Separate group profile | Prevents incorrectly treating pairwise PQXDH/Double Ratchet as a complete group-encryption solution. |

## Security Constraints

1. A bundle response contains no private key material and no session secret.
2. A client must validate the signed EC and Kyber prekey signatures before deriving a session and must abort on failure.
3. A one-time EC or Kyber prekey is allocated once only; it is never restored after a failed remote delivery. A signed Kyber last-resort prekey is returned only when no Kyber one-time prekey is available.
4. Device identity changes must be visible to the client and invalidate prior user-verification status.
5. Message envelopes have associated metadata bound to sender/recipient device, conversation, and protocol version by the client cryptographic adapter; the server authorizes routing but does not verify/decrypt cryptographic payloads.
6. The first server profile is PQXDH plus Double Ratchet and must use a reviewed library implementation with target-platform/operational approval.
7. Group encryption is deferred to a separately versioned protocol profile. Existing group membership/version checks remain mandatory but are not a group-encryption protocol.

## Deferred Work

Before any product can claim Signal-protocol-based end-to-end encryption, it needs an approved maintained client library for every target platform, legal review of dependency licensing/distribution, device secure-storage implementation, durable encrypted ratchet-state storage, X3DH/PQXDH prekey generation/rotation/deletion, atomic replenishment, identity fingerprint and QR comparison UX, key-change blocking/reconfirmation UX, sealed-sender/privacy review if adopted, attachment encryption design, multi-device fan-out and recovery policy, group sender-key or MLS evaluation, formal/integration interoperability testing, cryptographic code review, external penetration test, and user-facing security documentation.

## References

[1] https://signal.org/docs/specifications/x3dh/

[2] https://signal.org/docs/specifications/doubleratchet/

[3] https://github.com/signalapp/libsignal
