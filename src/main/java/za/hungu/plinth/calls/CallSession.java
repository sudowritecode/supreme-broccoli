package za.hungu.plinth.calls;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "call_sessions")
public class CallSession {

    @Id
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "started_by_device_id", nullable = false)
    private UUID startedByDeviceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CallSessionStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "provider_session_id", length = 256)
    private String providerSessionId;

    protected CallSession() {
    }

    private CallSession(UUID conversationId, UUID startedByDeviceId, Instant startedAt) {
        this.id = UUID.randomUUID();
        this.conversationId = conversationId;
        this.startedByDeviceId = startedByDeviceId;
        this.status = CallSessionStatus.ACTIVE;
        this.startedAt = startedAt;
    }

    public static CallSession start(UUID conversationId, UUID startedByDeviceId, Instant startedAt) {
        return new CallSession(conversationId, startedByDeviceId, startedAt);
    }

    public void end(Instant endedAt) {
        if (status == CallSessionStatus.ENDED) {
            return;
        }
        this.status = CallSessionStatus.ENDED;
        this.endedAt = endedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public UUID getStartedByDeviceId() {
        return startedByDeviceId;
    }

    public CallSessionStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }
}
