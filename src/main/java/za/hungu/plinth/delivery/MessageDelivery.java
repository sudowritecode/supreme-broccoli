package za.hungu.plinth.delivery;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "message_deliveries")
public class MessageDelivery {

    @Id
    private UUID id;

    @Column(name = "outbox_id", nullable = false)
    private UUID outboxId;

    @Column(name = "recipient_device_id", nullable = false)
    private UUID recipientDeviceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MessageDeliveryStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(nullable = false)
    private int attempts;

    protected MessageDelivery() {
    }

    private MessageDelivery(UUID outboxId, UUID recipientDeviceId, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.outboxId = outboxId;
        this.recipientDeviceId = recipientDeviceId;
        this.status = MessageDeliveryStatus.PENDING;
        this.createdAt = createdAt;
        this.attempts = 0;
    }

    public static MessageDelivery pending(UUID outboxId, UUID recipientDeviceId, Instant createdAt) {
        return new MessageDelivery(outboxId, recipientDeviceId, createdAt);
    }

    public void recordAttempt(Instant attemptedAt) {
        this.lastAttemptAt = attemptedAt;
        this.attempts++;
    }

    public boolean markDelivered(Instant deliveredAt) {
        if (status == MessageDeliveryStatus.DELIVERED) {
            return false;
        }
        this.status = MessageDeliveryStatus.DELIVERED;
        this.deliveredAt = deliveredAt;
        return true;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOutboxId() {
        return outboxId;
    }

    public UUID getRecipientDeviceId() {
        return recipientDeviceId;
    }

    public MessageDeliveryStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public int getAttempts() {
        return attempts;
    }
}
