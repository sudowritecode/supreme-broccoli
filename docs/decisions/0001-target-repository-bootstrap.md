# ADR-0001: Bootstrap the Empty Target Repository as the Plinth Platform Backend

**Status:** Accepted  
**Date:** 18 August 2026  
**Decision owner:** Manus AI, per user instruction to proceed autonomously and record decisions for later review.

## Context

The selected `sudowritecode/supreme-broccoli` repository was cloned successfully and contains no commits or source files. There is therefore no pre-existing application architecture, build system, migration history, security boundary, or deployment configuration to preserve.

A previously verified private-text backend foundation exists in the working environment. It uses Java 21, Spring Boot 3.4, Maven Wrapper, Flyway, PostgreSQL/H2, RabbitMQ, authenticated ciphertext ingress, transactional outbox delivery, explicit device sessions, authenticated WebSocket delivery, durable recipient-device delivery state, reconnect replay, and device revocation.

## Decision

The target repository will be bootstrapped at its root as the **Plinth modular Spring Boot backend** using the verified private-text foundation as the initial codebase. The migration history and test suite will be retained, because they encode the existing security boundaries and data-model evolution. The initial commit will include the repository audit, security/data-flow documentation, and decision records.

## Consequences

| Consequence | Rationale |
|---|---|
| Java 21 and Spring Boot 3.4 are the initial runtime baseline | The source foundation has been compiled and integration-tested on that stack. |
| The root is a backend service, not a mobile application | The currently completed and testable value is the text/realtime service. Mobile encryption/client work remains a later, separately verified slice. |
| Ciphertext-only transport is preserved | The server must not introduce plaintext message endpoints, logs, or fallbacks. |
| Legacy development device tokens remain compatibility-only | They allow the initial backend registration flow to work; durable device sessions provide the next identity boundary. Production recovery/linking remains unfinished. |
| Advanced social and financial features remain disabled/unimplemented until their prerequisite slices | Groups must precede rooms; calls require media-provider selection; games/mini-apps require isolation; financial capability requires a regulated partner and activation gate. |

## Rejected Alternatives

| Alternative | Reason not selected |
|---|---|
| Start a blank framework without the verified private-text foundation | It would discard working tests, migrations, and security boundaries, increasing reimplementation risk. |
| Build calls, rooms, games, or payment features before text/device delivery exists | It would violate the approved text-first, privacy-first delivery sequence. |
| Treat the empty repository as a frontend/mobile repository | No existing client stack was present, while the verified backend is ready to establish the core platform contract. |

## Review Triggers

Revisit this decision when a production mobile client repository is introduced, when end-to-end encryption library integration is selected, or when scaling requires separating the WebSocket gateway/delivery worker from the modular monolith.
