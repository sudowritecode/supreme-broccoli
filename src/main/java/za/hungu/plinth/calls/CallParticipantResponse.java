package za.hungu.plinth.calls;

import java.util.UUID;

public record CallParticipantResponse(
        UUID callSessionId,
        UUID accountId,
        CallParticipantStatus status,
        boolean mediaProviderConfigured
) {
}
