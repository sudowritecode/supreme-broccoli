# ADR-0004: Use Invite-Only Interest-Matched Rooms With Host Approval

**Status:** Accepted  
**Date:** 18 August 2026  
**Decision owner:** Manus AI, per user instruction to proceed autonomously and record decisions for review.

## Context

The roadmap calls for a Houseparty-inspired room feature, but the clarified product requirement is **invite-only rooms with interest matching**, not fully random rooms. Text, private groups, and call-session orchestration are now in place. Public room discovery or anonymous stranger entry would add substantial safety, moderation, privacy, and operational risk before basic room controls are proven.

## Decision

Implement rooms as short-lived, private social contexts with explicit host/co-host control. A user may create a room, invite a known account, or receive a limited suggestion when both users have opted into a shared non-sensitive interest tag. Interest matching is only a suggestion: it produces a pending entry request, and the host must admit the user. No endpoint, user flow, or matching policy will provide a random join or global list of rooms.

The first room slice includes opt-in interest tags, room topic/capacity, host/co-host ownership, invite/request/admit state, leave/remove/end, block enforcement, report record creation, and deterministic matching reasons. It does not enable media, recording, automatic stranger admission, exact interest disclosure, location matching, public feeds, or retained historical presence.

## Consequences

| Decision | Consequence |
|---|---|
| Host approval is mandatory for interest matches | A matching result cannot place a user directly into a room. |
| Interests are opt-in tags only | Matching cannot use message content, sensitive characteristics, precise location, finance data, or inferred profile data. |
| Match results carry explanation codes | The client can show a general reason without exposing a room member's private settings. |
| Blocks override invites and matching | A blocked account cannot request, be admitted to, or receive a room suggestion involving the blocker. |
| Room participation is short-lived | No persistent presence feed or public room directory is created. |
| No media adapter is integrated in this slice | Existing call-session orchestration may be linked later after room membership and moderation prove stable. |

## Acceptance Criteria

1. A host can create and end a room with a topic, capacity, and selected interest tags.
2. A member with a valid host invite may request entry, but only the host/co-host can admit them.
3. Interest matching returns only eligible room suggestions with a deterministic reason code and no direct admission.
4. Random discovery is unavailable by design and test.
5. A block prevents room suggestion, entry request, and host admission.
6. Host/co-host can remove a participant; removed participants cannot re-enter via the same room state.
7. Reports are recorded as typed moderation events without plaintext chat/media capture.

## Deferred Work

Future work may link a room to a call session, add managed room-call media, support a curated game-session launch, define an on-call moderation workflow, introduce ephemeral presence via Redis, and perform matching quality/fairness review. Those steps remain blocked until this safety-focused room lifecycle is tested.
