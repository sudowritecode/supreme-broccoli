# Release-Readiness Runbook

## Current Baseline

The repository has a repeatable Java 21 continuous-integration check and a growing vertical-slice integration suite. The check runs `./mvnw -B clean package`, which compiles the service, applies every Flyway migration to isolated test databases, and runs the available tests. This is a **development verification baseline**, not evidence that the product is production-ready.

| Control | Current state | Operational meaning |
|---|---|---|
| CI build/test | Implemented on GitHub push and pull request | Changes receive repeatable compile/migration/test validation. |
| Maven Wrapper | Implemented | Local and CI builds use the repository-pinned build entry point. |
| Flyway migrations | Implemented through V9 | Schema evolution is versioned; shared-environment migration rehearsal is still required. |
| Actuator health | Implemented | Basic process/dependency reachability endpoint only. |
| Ciphertext-only message ingress | Implemented | The backend avoids message plaintext but is not yet a fully audited E2EE system. |
| Feature gates | Baseline configuration present | Messaging broker, mini apps, sandbox payments and live payment refusal use explicit settings. |
| Automated deployment | Not implemented | No push deploys an environment. |
| Live media/payments | Not implemented | Calls are orchestration only; payments are sandbox only. |

## Local Release Verification

Run this before opening a pull request or assembling any non-production release candidate.

```bash
./mvnw -B clean package
```

The TypeScript and Python mini-app SDK checks are separate from the Maven lifecycle.

```bash
cd sdks/javascript
pnpm install --frozen-lockfile
pnpm check

cd ../python
uv run --with cryptography python tests/test_verification.py
```

Use local Compose dependencies only for development. Do not reuse the supplied local passwords, default broker configuration, H2 database, or development device-token workflow in a shared or production environment.

## Environment Configuration

| Concern | Required production approach | Current repository position |
|---|---|---|
| Database | Managed PostgreSQL with encrypted backups, least-privilege migration/app roles, restore rehearsal, and connection security. | Docker Compose local dependency only. |
| Broker/cache | Managed RabbitMQ/Redis with TLS, credentials in a secret manager, alerting, and capacity policy. | Optional local configuration only. |
| Secrets | Managed secret store, rotation, access audit, and no secrets in artifacts or logs. | Environment variable placeholders only. |
| Mini apps | Keep disabled until developer review, sandbox, consent revocation, key rotation, and app-operations controls exist. | Disabled by default; signed foundation only. |
| Payments | Keep disabled and `live-enabled: false` until regulated partner, compliance, reconciliation, and all financial controls are complete. | Sandbox intent API only; live activation refused. |
| Calls/media | Select and review a media provider, TURN/SFU architecture, client permissions, and quality/abuse operations. | Session orchestration only. |

## Mandatory Launch Gates

The following gates block a public launch. None may be bypassed by a CI pass.

| Gate | Required evidence |
|---|---|
| Threat model and external penetration test | Reviewed report, remediated critical/high findings, and residual-risk approval. |
| Cryptography and identity review | Audited end-to-end encryption protocol integration, key verification/recovery policy, and mobile secure-storage implementation. |
| Privacy, retention, and deletion | Data inventory, retention schedule, deletion/export process, privacy review, and processor agreements as applicable. |
| Production infrastructure | Infrastructure-as-code, environment separation, encrypted backups, restore drill, capacity test, secret manager, TLS, and access control. |
| Observability and incident response | Central logs, metrics, traces, alerting, runbooks, on-call process, incident communications, and post-incident workflow. |
| Abuse and safety | Rate limits, bot/abuse mitigation, room/report moderation operations, account recovery, and escalation policy. |
| Mobile release | Security-reviewed Android/iOS clients, permission UX, push-notification privacy review, store policy compliance, and beta cohort. |
| Media provider | Provider selection, legal/privacy review, TURN/SFU security, session token policy, call quality monitoring, and failure recovery. |
| Mini-app program | App developer review, sandboxing, scope consent/revocation, key rotation, suspension, audit logs, and origin verification. |
| Financial partner | Regulated partner contract, compliance approvals, PCI/PII boundary, KYC/AML/sanctions/fraud controls as applicable, webhooks, reconciliation, ledger, disputes, and financial operations. |

## Deployment Procedure After Gates Are Met

1. Create a release candidate from a reviewed commit that has passed CI.
2. Run the full local release verification commands and retain results with the release record.
3. Rehearse all pending Flyway migrations against a production-like database backup or anonymized replica, including rollback/forward-fix plan.
4. Deploy to an isolated staging environment with production-equivalent secrets and managed dependencies.
5. Execute API, WebSocket delivery, device revocation, group, room, game, mini-app, and sandbox-payment smoke tests with non-sensitive fixtures.
6. Review logs, metrics, error budgets, and migration results. Obtain required security, privacy, compliance, and operational sign-offs.
7. Use an approved progressive rollout plan with a documented rollback decision owner. Do not enable mini-app or payment features merely because the core API deploys.
8. Record the deployed commit, configuration version, migration version, on-call owner, and rollback artifact.

## Explicit Non-Launch Statement

> This repository is an MVP backend foundation. It is not authorized or prepared to process real payments, provide a production media service, host untrusted mini apps, or claim audited Signal-compatible end-to-end encryption. A public launch remains blocked on the gates above.
