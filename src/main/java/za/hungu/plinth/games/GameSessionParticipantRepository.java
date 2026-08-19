package za.hungu.plinth.games;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GameSessionParticipantRepository extends JpaRepository<GameSessionParticipant, GameSessionParticipant.GameSessionParticipantId> {
    Optional<GameSessionParticipant> findByGameSessionIdAndAccountId(UUID gameSessionId, UUID accountId);
    boolean existsByGameSessionIdAndAccountIdAndStatus(
            UUID gameSessionId,
            UUID accountId,
            GameSessionParticipantStatus status
    );
}
