package za.hungu.plinth.games;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "game_session_participants")
@IdClass(GameSessionParticipant.GameSessionParticipantId.class)
public class GameSessionParticipant {
    @Id
    @Column(name = "game_session_id", nullable = false)
    private UUID gameSessionId;

    @Id
    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "participant_status", nullable = false, length = 16)
    private GameSessionParticipantStatus status;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    protected GameSessionParticipant() {
    }

    private GameSessionParticipant(UUID gameSessionId, UUID accountId, Instant joinedAt) {
        this.gameSessionId = gameSessionId;
        this.accountId = accountId;
        this.status = GameSessionParticipantStatus.JOINED;
        this.joinedAt = joinedAt;
    }

    public static GameSessionParticipant join(UUID gameSessionId, UUID accountId, Instant joinedAt) {
        return new GameSessionParticipant(gameSessionId, accountId, joinedAt);
    }

    public void leave(Instant leftAt) {
        if (status == GameSessionParticipantStatus.JOINED) {
            status = GameSessionParticipantStatus.LEFT;
            this.leftAt = leftAt;
        }
    }

    public UUID getGameSessionId() { return gameSessionId; }
    public UUID getAccountId() { return accountId; }
    public GameSessionParticipantStatus getStatus() { return status; }
    public Instant getJoinedAt() { return joinedAt; }
    public Instant getLeftAt() { return leftAt; }

    public static class GameSessionParticipantId implements Serializable {
        private UUID gameSessionId;
        private UUID accountId;

        public GameSessionParticipantId() {
        }

        public GameSessionParticipantId(UUID gameSessionId, UUID accountId) {
            this.gameSessionId = gameSessionId;
            this.accountId = accountId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof GameSessionParticipantId that)) return false;
            return Objects.equals(gameSessionId, that.gameSessionId) && Objects.equals(accountId, that.accountId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(gameSessionId, accountId);
        }
    }
}
