package za.hungu.plinth.games;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "game_sessions")
public class GameSession {
    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_id", nullable = false, length = 40)
    private CuratedGameId gameId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 16)
    private GameSessionSourceType sourceType;

    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    @Column(name = "started_by_account_id", nullable = false)
    private UUID startedByAccountId;

    @Column(name = "started_by_device_id", nullable = false)
    private UUID startedByDeviceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GameSessionStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    protected GameSession() {
    }

    private GameSession(UUID id, CuratedGameId gameId, GameSessionSourceType sourceType, UUID sourceId, UUID startedByAccountId, UUID startedByDeviceId, Instant startedAt) {
        this.id = id;
        this.gameId = gameId;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.startedByAccountId = startedByAccountId;
        this.startedByDeviceId = startedByDeviceId;
        this.status = GameSessionStatus.ACTIVE;
        this.startedAt = startedAt;
    }

    public static GameSession start(CuratedGameId gameId, GameSessionSourceType sourceType, UUID sourceId, UUID startedByAccountId, UUID startedByDeviceId, Instant startedAt) {
        return new GameSession(UUID.randomUUID(), gameId, sourceType, sourceId, startedByAccountId, startedByDeviceId, startedAt);
    }

    public void end(Instant endedAt) {
        if (status == GameSessionStatus.ACTIVE) {
            status = GameSessionStatus.ENDED;
            this.endedAt = endedAt;
        }
    }

    public UUID getId() { return id; }
    public CuratedGameId getGameId() { return gameId; }
    public GameSessionSourceType getSourceType() { return sourceType; }
    public UUID getSourceId() { return sourceId; }
    public UUID getStartedByAccountId() { return startedByAccountId; }
    public UUID getStartedByDeviceId() { return startedByDeviceId; }
    public GameSessionStatus getStatus() { return status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getEndedAt() { return endedAt; }
}
