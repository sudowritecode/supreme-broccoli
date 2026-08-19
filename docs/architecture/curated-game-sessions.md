# Curated Game Sessions

## Scope

The platform now provides **private-context game-session orchestration**, not a downloadable or browser-playable game client. A server-owned catalog supplies a bounded list of approved social game definitions. An authorised user can start a durable session inside an existing private room or direct/private-group conversation. The system records session and participant lifecycle state only; it stores no moves, scores, executable bundle, launch URL, user-created game package, wagering data, or payment data.

| Capability | Implemented behaviour |
|---|---|
| Curated catalog | Authenticated users can read a fixed catalog containing `WORD_CHAIN`, a non-monetized placeholder for a future first-party client. |
| Room launch | Only an admitted room `HOST` or `CO_HOST` can start one active game session for that room. |
| Conversation launch | Any active direct/private-group conversation member can start one active session for that conversation. |
| Join | A caller may join only when they remain an admitted participant in the active room or an active conversation member. |
| Leave | A joined participant can leave a currently active session. |
| End | The session starter may end the session. A current room host/co-host may also end a room-based session. |
| Closed source | An ended room blocks new game-session joins and launches. An inactive game session blocks join/leave/end transitions. |
| Public discovery | No route exposes a game lobby, global session list, random matching, game bundle, external URL, payment, or wagering interface. |

## API Contract

Every route requires `X-Device-Token` authentication.

| Method and route | Purpose |
|---|---|
| `GET /api/v1/games/catalog` | Read the closed curated catalog. |
| `POST /api/v1/games/rooms/{roomId}/sessions` | Start a game session as an admitted room host/co-host. |
| `POST /api/v1/games/conversations/{conversationId}/sessions` | Start a session as an active private-conversation member. |
| `POST /api/v1/games/sessions/{gameSessionId}/join` | Join only with current source-context membership. |
| `POST /api/v1/games/sessions/{gameSessionId}/leave` | Leave the active session. |
| `POST /api/v1/games/sessions/{gameSessionId}/end` | End as the starter or a current room moderator. |

## Lifecycle and Authorisation

```text
Eligible private-context member
  -> starts GameSession ACTIVE
  -> starter joins automatically
  -> other current eligible member joins
  -> participant may leave -> LEFT
  -> starter or room moderator ends -> GameSession ENDED

Room ends -> future game joins/launches rejected
Source membership ends -> future game joins rejected
```

Room games reuse the room module's strict `ACTIVE` room and `ADMITTED` participant checks. They require `HOST` or `CO_HOST` only for launch. Conversation games reuse the active membership checks for both direct and group conversations. The API returns `playableClientConfigured: false` so clients cannot misinterpret orchestration success as a playable game handoff.

> A game session is a private-context coordination record. It is **not** a game executable, a public game lobby, or a financial product.

## Data Model

| Table | Purpose |
|---|---|
| `game_sessions` | Curated game ID, private source type/ID, starter account/device provenance, and active/ended lifecycle. |
| `game_session_participants` | Per-account joined/left participation state. |

## Deferred Work

A subsequent browser-game milestone must build a real first-party playable client through a dedicated game-development workflow. That work needs original art, visual verification, an accessibility review, age/content policy, abuse controls, privacy-minimized gameplay telemetry, deterministic reconciliation, multi-device state synchronization, and a separate ADR. Third-party game providers require licensing, security, privacy, content-rating, and data-processing assessment before an adapter or launch credential may be added.
