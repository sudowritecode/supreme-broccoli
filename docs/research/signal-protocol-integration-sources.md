# Signal Protocol Integration Sources

**Research date:** 19 August 2026

## Official Sources Consulted

| Source | URL | Key finding used for the integration decision |
|---|---|---|
| Signal libsignal repository | https://github.com/signalapp/libsignal | `libsignal` exposes Java, Swift, and TypeScript APIs backed by Rust; it implements the Signal protocol including Double Ratchet. Its README also states that use outside Signal is unsupported and APIs may change without notice. The repository is AGPL-3.0 licensed. |
| Double Ratchet specification | https://signal.org/docs/specifications/doubleratchet/ | After an initial shared secret, Double Ratchet derives per-message keys through symmetric chains and mixes fresh DH outputs through a root chain for forward security and break-in recovery. It includes ratchet public keys and counters for out-of-order/skipped messages, which remain client-side cryptographic state. |
| X3DH specification | https://signal.org/docs/specifications/x3dh/ | Asynchronous initialization relies on an identity key, signed prekey, signed-prekey signature, optional one-time prekey, and an initiator ephemeral key. The server publishes recipient bundles and should return/delete one-time prekeys atomically. The initiator verifies the prekey signature before session establishment. Identity-key comparison through an authenticated channel is needed for a cryptographic identity guarantee. |
| PQXDH specification | https://signal.org/docs/specifications/pqxdh/ | Modern post-quantum initialization extends the bundle with signed PQ last-resort/one-time KEM prekeys. It still requires signature validation and prekey consumption. The current system will not claim PQXDH support until a chosen maintained client library supplies it consistently across target clients. |

## Design Implications

1. **Do not implement cryptographic primitives in the Spring server.** Server responsibility is a transparent public-key directory, signed-prekey directory, atomic one-time-prekey allocation, opaque protocol-envelope delivery, and public verification-state synchronization.
2. **Do not persist client private keys, ratchet state, plaintext, or derived message/session keys on the server.** Device clients must keep these in platform secure storage and use a reviewed library.
3. **Use a protocol-versioned server contract.** The first server implementation will support registration and bundle delivery for a client-selected `SIGNAL_X3DH_DOUBLE_RATCHET_V1` profile, while declaring it an integration target rather than asserting interoperability with Signal Messenger.
4. **Treat libsignal as an evaluated reference/library option, not an unqualified product dependency.** Its official README notes unsupported third-party use, API instability, and AGPL-3.0 licensing. Legal and maintenance review is required before distributing it in a commercial client.
5. **Safety numbers are client-calculated and user-verified.** The server may store an account/device identity-key fingerprint and a client-submitted verification state, but must not calculate verification codes from private material or falsely attest identity verification.
6. **Group encryption must be a separate client protocol/profile.** Group membership changes remain server-authorized; group-key distribution and sender-key/session state remain client-side, while the server accepts opaque versioned envelopes only.

## References

[1] https://github.com/signalapp/libsignal

[2] https://signal.org/docs/specifications/doubleratchet/

[3] https://signal.org/docs/specifications/x3dh/

[4] https://signal.org/docs/specifications/pqxdh/
