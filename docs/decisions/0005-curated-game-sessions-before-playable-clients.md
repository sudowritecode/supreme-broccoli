# ADR-0005: Create Curated Game Sessions Before Building Playable Clients

**Status:** Accepted  
**Date:** 18 August 2026  
**Decision owner:** Manus AI, per user instruction to proceed autonomously and record decisions for review.

## Context

The product requires a games area and social games that can be played from rooms. The current backend supports private conversations, group membership, invite-only rooms, and provider-neutral call sessions, but it has no game engine, content rating process, account-age signals, consumer client, or external game provider agreement. Building a random game lobby, embedding third-party games, or accepting executable game bundles now would bypass the established privacy and membership controls.

## Decision

Introduce a backend **curated game-session** foundation. A small server-owned catalog contains only approved game definitions. An authorised room moderator or active private-conversation member can start one active game session tied to that private context. Only current eligible members can join. A starter/host can end a session. The backend returns no executable game code, user-generated game package, external provider credential, wagering mechanism, or payment flow.

The initial catalog will use a non-monetized, turn-free social game placeholder (`WORD_CHAIN`) to validate session authorisation and client handoff contracts. It represents a future first-party playable client, not a production game implementation. All game state and gameplay mechanics remain outside this slice.

## Consequences

| Decision | Consequence |
|---|---|
| Catalog is server-owned | Clients cannot supply a game identifier or arbitrary launch URL. |
| Sessions attach to rooms or conversations | Existing membership and host controls govern access. |
| No random game discovery | A game is only visible to eligible members of its source context. |
| No gameplay state yet | The API cannot be mistaken for a released playable game. |
| No payments or wagering | The game foundation remains distinct from future regulated financial interfaces. |
| External games are deferred | A provider requires a separate security, privacy, content-rating, and licensing review. |

## Acceptance Criteria

1. The backend exposes a read-only curated catalog of approved game definitions.
2. A host/co-host can start a session for an active room; active conversation members can start one for a direct/private-group conversation.
3. A user must be an eligible current source-context member to join a session.
4. A removed/left room member, non-member, or user after room end cannot start or join the room's game session.
5. A game session is endable only by its starter or an authorised room moderator.
6. No API exposes a random lobby, executable game bundle, external URL, payment, or wagering field.

## Deferred Work

A later client-game milestone must follow the browser-game workflow, create original art assets, provide deterministic visual verification, and add a separate ADR covering age/content gating, accessibility, abuse handling, gameplay telemetry minimization, client integrity, and multi-device state synchronization. External partner games require provider due diligence before integration.
