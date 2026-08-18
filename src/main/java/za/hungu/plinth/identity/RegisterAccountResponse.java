package za.hungu.plinth.identity;

import java.time.Instant;
import java.util.UUID;

public record RegisterAccountResponse(
        UUID accountId,
        String username,
        UUID deviceId,
        String deviceToken,
        Instant createdAt
) {
}
