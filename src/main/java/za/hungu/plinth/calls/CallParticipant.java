package za.hungu.plinth.calls;

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
@Table(name = "call_participants")
@IdClass(CallParticipant.CallParticipantId.class)
public class CallParticipant {

    @Id
    @Column(name = "call_session_id")
    private UUID callSessionId;

    @Id
    @Column(name = "account_id")
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CallParticipantStatus status;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    protected CallParticipant() {
    }

    private CallParticipant(UUID callSessionId, UUID accountId, Instant joinedAt) {
        this.callSessionId = callSessionId;
        this.accountId = accountId;
        this.status = CallParticipantStatus.ACTIVE;
        this.joinedAt = joinedAt;
    }

    public static CallParticipant join(UUID callSessionId, UUID accountId, Instant joinedAt) {
        return new CallParticipant(callSessionId, accountId, joinedAt);
    }

    public boolean leave(Instant leftAt) {
        if (status != CallParticipantStatus.ACTIVE) {
            return false;
        }
        this.status = CallParticipantStatus.LEFT;
        this.leftAt = leftAt;
        return true;
    }

    public boolean remove(Instant removedAt) {
        if (status != CallParticipantStatus.ACTIVE) {
            return false;
        }
        this.status = CallParticipantStatus.REMOVED;
        this.leftAt = removedAt;
        return true;
    }

    public UUID getCallSessionId() {
        return callSessionId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public CallParticipantStatus getStatus() {
        return status;
    }

    public static final class CallParticipantId implements Serializable {

        private UUID callSessionId;
        private UUID accountId;

        public CallParticipantId() {
        }

        public CallParticipantId(UUID callSessionId, UUID accountId) {
            this.callSessionId = callSessionId;
            this.accountId = accountId;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof CallParticipantId that)) {
                return false;
            }
            return Objects.equals(callSessionId, that.callSessionId) && Objects.equals(accountId, that.accountId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(callSessionId, accountId);
        }
    }
}
