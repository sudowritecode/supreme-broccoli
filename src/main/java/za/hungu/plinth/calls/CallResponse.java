package za.hungu.plinth.calls;

import java.time.Instant;
import java.util.UUID;

public record CallResponse(
        UUID callSessionId,
        UUID conversationId,
        CallSessionStatus status,
        CallParticipantStatus callerParticipantStatus,
        boolean mediaProviderConfigured,
        Instant startedAt
) {
}
