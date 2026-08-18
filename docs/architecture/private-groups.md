# Private Groups

## Implemented Scope

Private groups extend the existing text-first backend without changing its ciphertext-only delivery boundary. A group is represented by a `GROUP` conversation type and uses the same encrypted-message ingress, durable outbox, recipient delivery state, and authenticated WebSocket flow as direct conversations.

The first group slice supports one group owner, optional future admins, members, invitation, acceptance, decline, owner/admin invitation authority, owner/admin removal authority, voluntary departure for non-owners, and ordered membership versions.

| Capability | Current rule |
|---|---|
| Discovery | No public directory, public channel, broadcast surface, or open invite link exists. |
| Creation | An authenticated device creates a named private group; its account becomes the active owner. |
| Invitation | Only owner/admin can invite a known username. The invitee must explicitly accept to become active. |
| Sending | The sender account and the recipient device's account must both be `ACTIVE` group members. |
| Removal | Owner can remove admin/member; admin can remove member; neither can remove an owner. |
| Departure | A non-owner may leave. Owner transfer is intentionally deferred, so the owner cannot leave in this slice. |
| Ordering | Every invitation, acceptance, decline, removal, and departure increments the group `membershipVersion`. |
| Content | The backend accepts and routes opaque ciphertext only. |

## HTTP Contract

| Method and route | Behaviour |
|---|---|
| `POST /api/v1/groups` | Create a private group; caller is owner. |
| `POST /api/v1/groups/{groupId}/invitations` | Owner/admin invites a username. |
| `POST /api/v1/groups/{groupId}/invitations/accept` | Invitee accepts their pending invitation. |
| `POST /api/v1/groups/{groupId}/invitations/decline` | Invitee declines their pending invitation. |
| `POST /api/v1/groups/{groupId}/leave` | Active non-owner leaves. |
| `DELETE /api/v1/groups/{groupId}/members/{accountId}` | Owner/admin removes an authorised target member. |
| `POST /api/v1/messages` | Existing ciphertext ingress now requires explicit `ACTIVE` membership for direct and group conversations. |

## State Model

```text
Owner creates group
  -> owner ACTIVE (membership version 1)
  -> owner/admin invites account (INVITED; version increments)
  -> invitee accepts (ACTIVE; version increments)
       -> can receive and send ciphertext envelopes
  -> invitee declines / member leaves / member removed
       -> no longer authorised for future group ciphertext envelopes
```

Membership records use a stable `(conversation_id, account_id)` key. Re-inviting a departed, removed, or declined member is intentionally rejected in this initial slice rather than silently overwriting history. A later iteration may add an auditable re-invitation/role-transfer policy after group encryption session implications are designed.

## Security and Privacy Constraints

1. The server derives sender identity from the authenticated device/session; group APIs never accept a caller-supplied sender identity.
2. Invitation status is not active membership. Invited or declined accounts cannot receive or send new group ciphertext envelopes.
3. Message ingress checks active membership for both the sender and the target recipient account at queue time.
4. Removal/departure prevents new queued envelopes after the membership transition. Existing encrypted deliveries follow their separate durable state/retention policy.
5. This backend does not create, inspect, or store group plaintext, private keys, group secret material, or a custom group encryption protocol.

## Deferred Work

The next group-security iteration must select an audited client-side group session/key-management design, define ordered membership-change events to client devices, handle multi-device fan-out, add ownership transfer, and establish a re-invite policy. Calls and invite-only rooms must consume the same active membership and version state rather than create parallel authorisation models.
