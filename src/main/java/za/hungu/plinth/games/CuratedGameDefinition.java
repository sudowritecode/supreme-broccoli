package za.hungu.plinth.games;

public record CuratedGameDefinition(
        CuratedGameId id,
        String displayName,
        String description,
        int minPlayers,
        int maxPlayers
) {
}
