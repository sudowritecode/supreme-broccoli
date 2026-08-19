# ADR-0006: Use Signed Mini-App Manifests and Narrow Platform Permissions

**Status:** Accepted  
**Date:** 19 August 2026  
**Decision owner:** Manus AI, per user instruction to proceed autonomously and record decisions for review.

## Context

The product requires JavaScript/TypeScript and Python mini-app SDKs. The platform already handles private text, rooms, and curated game-session coordination, but it has no client sandbox, OAuth consent UI, developer portal, app-review operation, tenant isolation, or durable third-party credential model. An unrestricted mini-app bridge could expose device authentication tokens, private conversation membership, encrypted payloads, financial interfaces, or unaudited network execution to third-party code.

## Decision

Build a **foundation-only mini-app contract** centred on server-registered, signed manifests. A mini-app manifest identifies the issuer, app ID, version, approved origin, narrow permission set, and public signing key. The platform verifies an Ed25519 signature over canonical manifest content and issues a short-lived, one-time launch ticket only after validating the caller's consent to the app's declared permissions.

The first bridge exposes no private message content, ciphertext, raw device token, contact graph, room/member directory, payment interface, filesystem access, arbitrary network privilege, or executable bundle hosting. Permitted scopes are limited to `PROFILE_BASIC` and `CONTEXT_LAUNCH`. The backend records only app/session metadata necessary to validate a ticket. A browser client SDK will verify ticket expiry/nonce and invoke a host-provided, allowlisted bridge method; a Python SDK will verify signed manifests and launch tickets using the registered public key.

## Consequences

| Decision | Consequence |
|---|---|
| Server registration is required | A client cannot self-register arbitrary manifest metadata. |
| Ed25519 signed manifests | Clients and server can detect unsigned or modified manifest content without shared secrets. |
| Canonical payload | The signature excludes only its own signature field and uses a deterministic field encoding. |
| Narrow declared scopes | There is no route to request private communication, finance, device, or unrestricted networking permissions. |
| Explicit consent at launch | A user must submit the complete set of declared permissions at ticket creation. |
| Short-lived one-time tickets | A ticket can be consumed once and expires quickly; it is not a durable app credential. |
| SDKs verify only | The initial SDKs do not provide sandboxing, app hosting, arbitrary API calls, or payment actions. |

## Acceptance Criteria

1. An operator-managed registration endpoint accepts a manifest plus Ed25519 public key and rejects invalid signatures or disallowed permissions.
2. A signed manifest is retrievable by its immutable registered app ID/version.
3. An authenticated user can create a short-lived launch ticket only by consenting to exactly the manifest's permission set.
4. A ticket can be consumed once before expiry; expired or reused tickets are rejected.
5. The JS/TS SDK checks app ID, version, expiry, nonce, declared permissions, and uses an allowlisted bridge dispatch.
6. The Python SDK verifies a manifest signature and a launch-ticket signature using standard-library cryptography boundaries.
7. No mini-app route or SDK exports device tokens, message content/ciphertext, finance methods, user contact lists, or broad network execution.

## Deferred Work

Future mini-app work requires a developer console, operator authentication and review workflows, client sandboxing (CSP/iframe/WebView isolation), OAuth-style per-scope consent persistence/revocation, rate limits, audit logs, app suspension, origin attestations, mobile bridge implementation, secure remote key rotation, content/security scanning, and explicit finance capability policy. Each new scope must receive its own ADR and security review.
