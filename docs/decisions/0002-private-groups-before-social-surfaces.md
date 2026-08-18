# ADR-0002: Implement Private Groups as the Next Social Communication Slice

**Status:** Accepted  
**Date:** 18 August 2026  
**Decision owner:** Manus AI, per user instruction to proceed without permission requests and record decisions for review.

## Context

The target repository now has a verified private-text foundation: device-aware identity, consent-based direct conversations, ciphertext-only ingress, durable outbox/delivery state, authenticated WebSocket delivery, reconnect replay, and revocation.

The remaining roadmap includes private groups, voice/video calls, invite-only interest rooms, games, mini-apps, and partner-ready financial interfaces. Groups are the first dependency for controlled room membership and small-group calls. Implementing calls or rooms first would require re-creating group membership/role semantics later and would create unnecessary moderation risk.

## Decision

Implement a **private group conversation** slice next. The first release has explicit owner, admin, and member roles; invitation and approval; removal and leave; active-membership authorisation; versioned membership changes; and group ciphertext-envelope ingress. It does not provide public communities, broadcast channels, unbounded group discovery, server-readable message content, recordings, calls, room matching, game state, mini-app execution, or financial actions.

## Consequences

| Decision | Consequence |
|---|---|
| Reuse `conversations` and `conversation_members` | Direct and group messages use the same ciphertext-only ingress and delivery pipeline. |
| Add `conversation_type`, group name, roles, invitation state, and membership version | Group operations gain explicit state and safe ordering without breaking direct conversations. |
| Authorise group messages through active membership | Removed/left members cannot send new envelopes or receive newly addressed device deliveries. |
| Require owner/admin authority for invitations and removal | Moderation has predictable server-enforced controls. |
| Keep group encryption protocol integration separate | The server provides membership and ciphertext routing; it does not invent group cryptography. |

## Acceptance Criteria

1. A user can create a private group and becomes its owner.
2. An owner/admin can invite a user; the invitee accepts or declines.
3. Active group members can submit ciphertext envelopes only to devices belonging to active members.
4. A removed or departed member cannot submit a new group envelope.
5. An admin cannot remove an owner; an owner can remove an admin/member.
6. Membership changes are idempotent and carry a monotonically increasing group membership version.
7. Tests cover invitation, acceptance, removal, leave, unauthorised actions, and group encrypted ingress.

## Deferred Decisions

A later security review will select and integrate an audited group encryption/session strategy in the mobile clients. A later calls slice will use group membership versioning to authorise media credentials. Invite-only rooms will reuse the same host/co-host and removal primitives rather than inventing a separate social graph.
