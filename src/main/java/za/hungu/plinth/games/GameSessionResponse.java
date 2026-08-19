package za.hungu.plinth.games;

import java.time.Instant;
import java.util.UUID;

public record GameSessionResponse(
        UUID id,
        CuratedGameId gameId,
        GameSessionSourceType sourceType,
        UUID sourceId,
        GameSessionStatus status,
        UUID startedByAccountId,
        Instant startedAt,
        Instant endedAt,
        boolean playableClientConfigured
) {
}
