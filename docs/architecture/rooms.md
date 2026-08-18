# Invite-Only Interest-Matched Rooms

## Scope

Rooms are **private, short-lived social contexts**. They support an invite-only Houseparty-style interaction model without public room browsing or automatic stranger admission. The room layer manages membership, lobbies, host delegation, blocks, and typed reports. It does not transport text/media content, create a live audio/video session, record calls, expose location, or retain a presence feed.

| Capability | Implemented behaviour |
|---|---|
| Room creation | An authenticated device creates an `ACTIVE` room with a bounded topic, capacity, and selected interest tags. The creator is admitted as `HOST`. |
| Direct invite | A host or co-host can invite an **accepted contact**. The invite creates `LOBBY` state; it does not admit the recipient. |
| Interest suggestion | A user with opt-in tags can receive an eligible room suggestion carrying `SHARED_INTEREST`. The suggestion still requires an entry request and host/co-host approval. |
| Lobby admission | Only an admitted host or co-host can move a `LOBBY` participant to `ADMITTED`, subject to room capacity. |
| Co-host delegation | The host may appoint an admitted standard participant as `CO_HOST`. |
| Blocks | A host or co-host block immediately prevents subsequent suggestion, invitation, entry request, and admission, and removes a current non-host participant. |
| Reports | An admitted participant can create a report with a typed reason and optional account target. The system stores no plaintext narrative, text, or media content. |
| Discovery | There is intentionally no `GET /api/v1/rooms` directory, random join API, or auto-admission path. |

## API Contract

All routes require `X-Device-Token` authentication.

| Method and route | Purpose |
|---|---|
| `POST /api/v1/rooms` | Create an invite-only room. |
| `PUT /api/v1/rooms/interest-preferences` | Replace the caller's voluntary matching tags. |
| `GET /api/v1/rooms/suggestions` | Return only direct invitations and shared-interest suggestions eligible for the caller. |
| `POST /api/v1/rooms/{roomId}/invitations` | Add an accepted contact to the lobby. |
| `POST /api/v1/rooms/{roomId}/entry-requests` | Request lobby entry through shared opted-in interests. |
| `POST /api/v1/rooms/{roomId}/participants/{accountId}/admit` | Admit a lobby participant as host or co-host. |
| `POST /api/v1/rooms/{roomId}/participants/{accountId}/co-host` | Appoint an admitted participant as co-host; host only. |
| `POST /api/v1/rooms/{roomId}/leave` | Leave a room; the host must end it instead. |
| `DELETE /api/v1/rooms/{roomId}/participants/{accountId}` | Remove a non-host participant as host/co-host. |
| `POST /api/v1/rooms/{roomId}/blocks` | Block and, when applicable, remove an account as host/co-host. |
| `POST /api/v1/rooms/{roomId}/reports` | File a typed, content-free room report as an admitted participant. |
| `POST /api/v1/rooms/{roomId}/end` | End the room; host only. |

## Participation State

```text
HOST creates room -> HOST / ADMITTED

Accepted contact invite OR shared-interest entry request
    -> PARTICIPANT / LOBBY
    -> host or co-host admits
    -> PARTICIPANT / ADMITTED
    -> participant leaves -> LEFT
    -> host/co-host removes or blocks -> REMOVED

HOST appoints admitted participant -> CO_HOST / ADMITTED
HOST ends the room -> room ENDED; no further entry or controls
```

## Authorisation and Safety Rules

The device token is resolved before every room operation, so a revoked device cannot use these APIs. Room-level controls then require durable participant state. Only `HOST` and `CO_HOST` participants in `ADMITTED` state can invite, admit, remove, or block. Only the `HOST` can appoint a co-host or end the room.

Interest tags are deliberately limited to user-provided, normalized tags. The matching service does not inspect message ciphertext, profile inference, location, sensitive characteristics, payment information, or media. It emits an explanation code (`DIRECT_INVITE` or `SHARED_INTEREST`) rather than exposing who selected which underlying preference.

> A matching result is an **invitation to request review**, not permission to enter a room.

Blocks are checked before invitation, request, suggestion, and admission. An account with an existing participation record cannot create a second entry path for that room; this also prevents a removed participant from re-entering through another matching route.

## Data Model

| Table | Purpose |
|---|---|
| `rooms` | Room lifecycle, capacity, and host device/account provenance. |
| `room_interest_tags` | Tags selected for a specific room. |
| `account_interest_preferences` | Voluntary account-level tags used for matching only. |
| `room_participants` | Composite room/account membership state, role, invite provenance, and timestamps. |
| `room_blocks` | Room-local host/co-host safety blocks. |
| `room_reports` | Typed moderation events without a content field. |

## Deferred Work

Future iterations may link an admitted room to the existing provider-neutral call-session orchestration, add a selected SFU/TURN implementation, launch curated game sessions from a room, introduce bounded Redis-backed ephemeral presence, define moderator case workflows, and conduct matching quality/fairness review. Each requires a separate ADR and must preserve the no-random-discovery, host-approval, and content-minimization invariants.
