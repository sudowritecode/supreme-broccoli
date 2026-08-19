package za.hungu.plinth.games;

import java.time.Instant;
import java.util.UUID;

public record GameSessionParticipantResponse(
        UUID gameSessionId,
        UUID accountId,
        GameSessionParticipantStatus status,
        Instant joinedAt,
        Instant leftAt
) {
}
