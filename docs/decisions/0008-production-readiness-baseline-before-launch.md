# ADR-0008: Establish a Production-Readiness Baseline Before Launch

**Status:** Accepted  
**Date:** 19 August 2026  
**Decision owner:** Manus AI, per user instruction to proceed autonomously and record decisions for review.

## Context

The backend now contains the planned product foundations: private messaging, groups, call orchestration, invite-only rooms, curated game sessions, signed mini-app manifests, and sandbox-only payment intents. It still lacks a continuous integration workflow, an automated security scanning policy, managed production deployment, secret manager, alerting backend, incident response process, mobile clients, penetration test, external compliance review, and real financial/media partners.

Treating passing local integration tests as launch readiness would be unsafe. At the same time, the repository needs a repeatable baseline so future work cannot silently bypass build, migration, or test verification.

## Decision

Add a GitHub Actions continuous-integration workflow that runs on push and pull request, executes the Maven wrapper's clean package lifecycle using Java 21, and uploads Surefire reports when the job fails. Add an explicit release-readiness runbook that names the deployment configuration, migration, secret, observability, security, privacy, data-retention, media-provider, mini-app, financial-partner, mobile-client, and incident-response work still required before production launch.

The CI workflow is verification-only. It does not publish containers, deploy infrastructure, provision secrets, run database migrations against a shared environment, invoke external providers, activate live payments, or schedule background work. Production deployment remains manual and blocked on the runbook gates.

## Consequences

| Decision | Consequence |
|---|---|
| CI executes `clean package` | Every change must compile, apply Flyway migrations in isolated tests, and pass the complete test suite. |
| Failure reports are retained | Contributors can inspect test failures without reproducing locally. |
| No automatic deployment | A code push cannot mutate a production environment or activate an external provider. |
| Explicit release gates | Missing hardening work is visible rather than being implied complete by the MVP codebase. |
| Health remains basic | Current health endpoints prove process/dependency reachability only; production SLO monitoring is still required. |

## Acceptance Criteria

1. Pushes and pull requests trigger a Java 21 Maven clean-package verification workflow.
2. CI stores Surefire reports on failure.
3. The repository documents local verification, required configuration, migration sequence, and launch blockers.
4. The runbook explicitly identifies that live payments, real-time media, mini-app production activation, and automated deployment are not production-ready.
5. No workflow or configuration can deploy, schedule, or activate live financial processing in this slice.

## Deferred Work

The next readiness stages include SBOM/dependency scanning, SAST/secret scanning, container image creation and signing, environment-specific deployment pipeline, infrastructure as code, managed PostgreSQL/RabbitMQ/Redis, centralized logs, metrics/tracing/alerting, backup/restore drills, rate limiting, abuse controls, DDoS/WAF design, secret manager, key rotation, data-retention/deletion operations, threat modeling, external penetration test, privacy/compliance review, app-store client release process, real provider selection, and staged load/performance testing.
