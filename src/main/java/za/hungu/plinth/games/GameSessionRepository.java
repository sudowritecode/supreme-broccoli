package za.hungu.plinth.games;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GameSessionRepository extends JpaRepository<GameSession, UUID> {
    Optional<GameSession> findBySourceTypeAndSourceIdAndStatus(
            GameSessionSourceType sourceType,
            UUID sourceId,
            GameSessionStatus status
    );
}
