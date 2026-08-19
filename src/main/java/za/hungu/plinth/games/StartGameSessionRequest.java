package za.hungu.plinth.games;

import jakarta.validation.constraints.NotNull;

public record StartGameSessionRequest(@NotNull CuratedGameId gameId) {
}
