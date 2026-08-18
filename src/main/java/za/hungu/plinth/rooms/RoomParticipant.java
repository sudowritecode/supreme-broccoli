package za.hungu.plinth.rooms;

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
@Table(name = "room_participants")
@IdClass(RoomParticipant.RoomParticipantId.class)
public class RoomParticipant {
    @Id
    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Id
    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "participant_role", nullable = false, length = 16)
    private RoomParticipantRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "participant_status", nullable = false, length = 16)
    private RoomParticipantStatus status;

    @Column(name = "invited_by_account_id")
    private UUID invitedByAccountId;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "admitted_at")
    private Instant admittedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    @Column(name = "removed_at")
    private Instant removedAt;

    protected RoomParticipant() {
    }

    private RoomParticipant(UUID roomId, UUID accountId, RoomParticipantRole role, RoomParticipantStatus status, UUID invitedByAccountId, Instant requestedAt, Instant admittedAt) {
        this.roomId = roomId;
        this.accountId = accountId;
        this.role = role;
        this.status = status;
        this.invitedByAccountId = invitedByAccountId;
        this.requestedAt = requestedAt;
        this.admittedAt = admittedAt;
    }

    public static RoomParticipant host(UUID roomId, UUID accountId, Instant createdAt) {
        return new RoomParticipant(roomId, accountId, RoomParticipantRole.HOST, RoomParticipantStatus.ADMITTED, accountId, createdAt, createdAt);
    }

    public static RoomParticipant lobby(UUID roomId, UUID accountId, UUID invitedByAccountId, Instant requestedAt) {
        return new RoomParticipant(roomId, accountId, RoomParticipantRole.PARTICIPANT, RoomParticipantStatus.LOBBY, invitedByAccountId, requestedAt, null);
    }

    public void admit(Instant admittedAt) {
        if (status != RoomParticipantStatus.LOBBY) {
            throw new IllegalStateException("Only lobby participants can be admitted.");
        }
        this.status = RoomParticipantStatus.ADMITTED;
        this.admittedAt = admittedAt;
    }

    public void leave(Instant leftAt) {
        if (status == RoomParticipantStatus.ADMITTED || status == RoomParticipantStatus.LOBBY) {
            this.status = RoomParticipantStatus.LEFT;
            this.leftAt = leftAt;
        }
    }

    public void remove(Instant removedAt) {
        if (role == RoomParticipantRole.HOST) {
            throw new IllegalStateException("The host cannot be removed from their room.");
        }
        this.status = RoomParticipantStatus.REMOVED;
        this.removedAt = removedAt;
    }

    public void promoteToCoHost() {
        if (status != RoomParticipantStatus.ADMITTED) {
            throw new IllegalStateException("Only admitted participants can be co-hosts.");
        }
        this.role = RoomParticipantRole.CO_HOST;
    }

    public UUID getRoomId() { return roomId; }
    public UUID getAccountId() { return accountId; }
    public RoomParticipantRole getRole() { return role; }
    public RoomParticipantStatus getStatus() { return status; }
    public UUID getInvitedByAccountId() { return invitedByAccountId; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getAdmittedAt() { return admittedAt; }
    public Instant getLeftAt() { return leftAt; }
    public Instant getRemovedAt() { return removedAt; }

    public static class RoomParticipantId implements Serializable {
        private UUID roomId;
        private UUID accountId;

        public RoomParticipantId() {
        }

        public RoomParticipantId(UUID roomId, UUID accountId) {
            this.roomId = roomId;
            this.accountId = accountId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof RoomParticipantId that)) return false;
            return Objects.equals(roomId, that.roomId) && Objects.equals(accountId, that.accountId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(roomId, accountId);
        }
    }
}
