package za.hungu.plinth.rooms;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "room_blocks")
@IdClass(RoomBlock.RoomBlockId.class)
public class RoomBlock {
    @Id
    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Id
    @Column(name = "blocked_account_id", nullable = false)
    private UUID blockedAccountId;

    @Column(name = "issued_by_account_id", nullable = false)
    private UUID issuedByAccountId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RoomBlock() {
    }

    private RoomBlock(UUID roomId, UUID blockedAccountId, UUID issuedByAccountId, Instant createdAt) {
        this.roomId = roomId;
        this.blockedAccountId = blockedAccountId;
        this.issuedByAccountId = issuedByAccountId;
        this.createdAt = createdAt;
    }

    public static RoomBlock create(UUID roomId, UUID blockedAccountId, UUID issuedByAccountId, Instant createdAt) {
        return new RoomBlock(roomId, blockedAccountId, issuedByAccountId, createdAt);
    }

    public UUID getRoomId() { return roomId; }
    public UUID getBlockedAccountId() { return blockedAccountId; }
    public UUID getIssuedByAccountId() { return issuedByAccountId; }
    public Instant getCreatedAt() { return createdAt; }

    public static class RoomBlockId implements Serializable {
        private UUID roomId;
        private UUID blockedAccountId;

        public RoomBlockId() {
        }

        public RoomBlockId(UUID roomId, UUID blockedAccountId) {
            this.roomId = roomId;
            this.blockedAccountId = blockedAccountId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof RoomBlockId that)) return false;
            return Objects.equals(roomId, that.roomId) && Objects.equals(blockedAccountId, that.blockedAccountId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(roomId, blockedAccountId);
        }
    }
}
