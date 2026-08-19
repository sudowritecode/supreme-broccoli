package za.hungu.plinth.games;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CuratedGameCatalog {
    private static final List<CuratedGameDefinition> GAMES = List.of(
            new CuratedGameDefinition(
                    CuratedGameId.WORD_CHAIN,
                    "Word Chain",
                    "A non-monetized social word game reserved for a future first-party playable client.",
                    2,
                    8
            )
    );

    public List<CuratedGameDefinition> list() {
        return GAMES;
    }

    public CuratedGameDefinition require(CuratedGameId gameId) {
        return GAMES.stream()
                .filter(game -> game.id() == gameId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Game is not in the curated catalog."));
    }
}
