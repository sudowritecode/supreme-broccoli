package za.hungu.plinth.rooms;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record RoomResponse(
        UUID id,
        String topic,
        int capacity,
        RoomStatus status,
        UUID hostAccountId,
        Set<String> interestTags,
        Instant createdAt,
        Instant endedAt
) {
}
