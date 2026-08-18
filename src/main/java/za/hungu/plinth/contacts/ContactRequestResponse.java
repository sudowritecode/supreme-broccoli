package za.hungu.plinth.contacts;

import java.time.Instant;
import java.util.UUID;

public record ContactRequestResponse(
        UUID requestId,
        UUID senderAccountId,
        UUID recipientAccountId,
        ContactRequestStatus status,
        Instant createdAt,
        Instant respondedAt,
        UUID conversationId
) {
}
