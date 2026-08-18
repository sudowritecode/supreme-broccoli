# Call Sessions

## Implemented Scope

The platform now provides durable **call-session orchestration** for active direct and private-group conversation members. It is a signalling and authorisation layer only. It does not create a live audio/video media connection, provider credential, recording, stream, or media-content telemetry.

| Capability | Implemented behaviour |
|---|---|
| Start | An active direct/group conversation member starts one active call session for that conversation. |
| Join | Another active member can join the active call session. |
| Leave | A joined participant can leave; the transition is durable and idempotent. |
| End | Only the device that started the call can end it in this slice. |
| Authentication | Every endpoint derives the account/device from `X-Device-Token`; revoked devices cannot start/join/leave/end calls. |
| Provider | The `CallMediaPort` contract defines the future media-adapter boundary; no implementation is enabled. |
| Membership | Call start/join checks the existing `ACTIVE` direct/group conversation membership state. |

## API Contract

| Method and route | Result |
|---|---|
| `POST /api/v1/calls/conversations/{conversationId}` | Start an active call session, or return the existing active session for the caller's conversation. |
| `POST /api/v1/calls/{callSessionId}/join` | Add an active conversation member as a call participant. |
| `POST /api/v1/calls/{callSessionId}/leave` | Mark the authenticated active participant as left. |
| `POST /api/v1/calls/{callSessionId}/end` | End the session when called by the starter device. |

Responses return `mediaProviderConfigured: false`. Clients must not treat a created call session as permission to start a WebRTC connection until a future media adapter provides a short-lived participant credential.

## State Model

```text
ACTIVE conversation member
  -> starts CallSession ACTIVE
  -> joins as CallParticipant ACTIVE
  -> participant leaves -> LEFT
  -> starter ends session -> CallSession ENDED

A revoked device cannot enter the authenticated call API.
A non-member cannot start or join.
An ENDED call cannot be joined.
```

## Provider Boundary

`CallMediaPort` requires future adapters to create a provider session, issue a short-lived participant credential, remove a participant, and end a session. The platform must perform the active membership/device checks before calling the port. Provider-specific room identifiers and credentials must not leak into generic conversation records or message payloads.

## Deferred Live-Media Work

A future call-media iteration must select an SFU/TURN provider, complete privacy/data-processing review, implement short-lived token issuance, add client microphone/camera permission UX, mobile network/reconnect handling, incoming-call ringing and content-free push wake-up, participant removal propagation, quality signals that exclude media content, operational SLOs, and failure/incident runbooks.
