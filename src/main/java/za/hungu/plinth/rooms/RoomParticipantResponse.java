package za.hungu.plinth.rooms;

import java.time.Instant;
import java.util.UUID;

public record RoomParticipantResponse(
        UUID roomId,
        UUID accountId,
        RoomParticipantRole role,
        RoomParticipantStatus status,
        UUID invitedByAccountId,
        Instant requestedAt,
        Instant admittedAt,
        Instant leftAt,
        Instant removedAt
) {
}
