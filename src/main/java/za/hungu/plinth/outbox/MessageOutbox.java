package za.hungu.plinth.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "message_outbox")
public class MessageOutbox {

    @Id
    private UUID id;

    @Column(name = "message_id", nullable = false, unique = true)
    private UUID messageId;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "sender_device_id", nullable = false)
    private UUID senderDeviceId;

    @Column(name = "recipient_device_id", nullable = false)
    private UUID recipientDeviceId;

    @Column(nullable = false, length = 1_000_000)
    private String ciphertext;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "last_error", length = 512)
    private String lastError;

    protected MessageOutbox() {
    }

    private MessageOutbox(
            UUID messageId,
            UUID conversationId,
            UUID senderDeviceId,
            UUID recipientDeviceId,
            String ciphertext,
            Instant receivedAt
    ) {
        this.id = UUID.randomUUID();
        this.messageId = messageId;
        this.conversationId = conversationId;
        this.senderDeviceId = senderDeviceId;
        this.recipientDeviceId = recipientDeviceId;
        this.ciphertext = ciphertext;
        this.receivedAt = receivedAt;
        this.status = OutboxStatus.PENDING;
        this.attempts = 0;
    }

    public static MessageOutbox queue(
            UUID messageId,
            UUID conversationId,
            UUID senderDeviceId,
            UUID recipientDeviceId,
            String ciphertext,
            Instant receivedAt
    ) {
        return new MessageOutbox(messageId, conversationId, senderDeviceId, recipientDeviceId, ciphertext, receivedAt);
    }

    public void markPublished(Instant publishedAt) {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = publishedAt;
        this.attempts++;
        this.lastError = null;
    }

    public void recordFailure(String error) {
        this.attempts++;
        this.lastError = error == null ? null : error.substring(0, Math.min(error.length(), 512));
    }

    public UUID getId() {
        return id;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public UUID getSenderDeviceId() {
        return senderDeviceId;
    }

    public UUID getRecipientDeviceId() {
        return recipientDeviceId;
    }

    public String getCiphertext() {
        return ciphertext;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public OutboxStatus getStatus() {
        return status;
    }
}
