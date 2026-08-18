# Repository Onboarding Audit

**Repository:** `sudowritecode/supreme-broccoli`  
**Audit date:** 18 August 2026  
**Audit mode:** Read-only before bootstrap

## Findings

| Area | Finding | Integration decision |
|---|---|---|
| Commit history | No commits exist on `main` | Create the initial repository history with the verified backend foundation and explicit decision records. |
| Source code | No application files exist | No legacy code needs migration, replacement, or compatibility adapters. |
| Build system | None | Adopt Maven Wrapper and Java 21 through the verified Spring Boot project. |
| Data schema | None | Start Flyway history at the proven V1–V3 schema sequence; never use unversioned schema creation. |
| Authentication | None | Introduce device registration, compatible development token, durable device-session, and revocation boundaries together. |
| Realtime transport | None | Introduce authenticated WebSocket delivery after durable ciphertext outbox/delivery state. |
| CI/CD | None | Document build/test commands now; add CI in a later production-readiness slice. |
| Secrets/environments | None | Commit only `.env.example`; do not seed credentials, runtime secrets, or production partner configuration. |

## Data-Flow Baseline to Establish

```text
Client device
  -> authenticated ciphertext ingress
  -> PostgreSQL-compatible durable outbox and recipient delivery state
  -> authenticated WebSocket recipient connection
  -> recipient acknowledgement / reconnect replay

No plaintext message content, private keys, raw persistent tokens, financial credentials, or raw provider credentials enter the backend.
```

## Bootstrap Exit Criteria

The bootstrap is complete only after the backend compiles, database migrations validate, the private-text and WebSocket integration tests pass, the data-flow documents are present, and the initial commit is published to the target repository.
