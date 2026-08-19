# ADR-0010: Version Group Encryption Separately from Pairwise PQXDH

**Status:** Accepted  
**Date:** 19 August 2026  
**Decision owner:** Manus AI, per the user’s instruction to proceed autonomously and record decisions for review.

## Context

Pairwise PQXDH plus Double Ratchet establishes and advances a session between two devices. It is not a complete group-encryption scheme. A group needs sender-key distribution to every active recipient device, per-sender ratcheting state, membership-change rekeying, device-add/remove handling, and binding between cryptographic group state and the server-authorized membership revision.

The currently evaluated official libsignal TypeScript declarations expose `SenderKeyDistributionMessage`, `processSenderKeyDistributionMessage`, `SenderKeyStore`, `groupEncrypt`, and `groupDecrypt`. Those APIs make sender-key distribution and group encryption client responsibilities. The server must not manufacture, decrypt, inspect, or retain sender keys or group plaintext. [1]

The existing platform already tracks ordered group membership versions and authorizes group ciphertext delivery only to active members. This is a necessary server authorization control but is not a cryptographic group-membership guarantee.

## Decision

Keep `GROUP` Signal envelopes disabled and rejected by the pairwise Signal ingress endpoint until a separately implemented `PLINTH_SIGNAL_GROUP_SENDER_KEYS_V1` client profile is ready. The future profile will use existing authorized pairwise PQXDH sessions to distribute signed sender-key distribution messages to every active recipient device. It will then use the reviewed client library’s sender-key APIs for opaque group-message ciphertexts.

Each client group payload must bind the application group/conversation identifier, **server membership version**, sender account/device, recipient device fan-out target, protocol profile, and sender-key distribution identifier within authenticated encrypted client data. Recipients must reject a group payload when the decrypted membership version does not match the authoritative membership snapshot they have fetched or when they lack a current sender-key distribution record for the claimed sender/device/version.

Any group membership addition, removal, departure, restoration, role-controlled removal, device addition, or identity-key change invalidates applicable sender-key state. Clients must distribute new sender keys to the current active device set through pairwise sessions before using the new group version. The server continues to accept only per-device opaque envelopes and never treats prior ciphertext as readable by a newly added member.

## Consequences

| Decision | Consequence |
|---|---|
| Group profile remains disabled | Prevents an incorrect claim that pairwise encryption automatically secures group messages. |
| Pairwise distribution first | A current PQXDH session is required for each recipient device before sender-key material is delivered. |
| Membership-version binding | Clients can detect stale or mismatched group-key state and force a refresh/rekey rather than silently rendering content. |
| Per-device fan-out | Multi-device recipients each receive the necessary distribution or group envelope through existing authorized delivery paths. |
| Rekey on membership/device changes | Removed or newly added participants do not receive future/previous cryptographic state by default. |
| Server stays opaque | The server authorizes device routing and records membership but has no sender-key or plaintext access. |

## Security Constraints

1. Do not enable a `GROUP` envelope until clients use a reviewed sender-key implementation and pass interoperability tests.
2. Do not reuse sender-key distributions across a membership-version change or device identity change.
3. Do not add recipients based solely on a client-provided member list; use the server-authorized active membership/device set.
4. Do not make historical ciphertext available to a device that was not an active member at send time.
5. Do not claim MLS, Signal group protocol, or forward secrecy for groups without a profile-specific review and test evidence.
6. Treat group history synchronization, backup, and recovery as a separate security design; neither follows automatically from sender-key encryption.

## Deferred Work

A group-encryption release needs a formal profile specification, client sender-key secure store, recipient-device enumeration API with membership versions, sender-key distribution and acknowledgement protocol, group-envelope schema, replay/order handling, membership-change rekey orchestration, offline-device retry/recovery behavior, group-history policy, multi-device verification UX, conformance vectors, cross-platform interop tests, load/fan-out controls, security review, penetration test, privacy review, and incident procedures.

## Reference

[1] https://github.com/signalapp/libsignal
