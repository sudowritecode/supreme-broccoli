package za.hungu.plinth.contacts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contact_requests")
public class ContactRequest {

    @Id
    private UUID id;

    @Column(name = "sender_account_id", nullable = false)
    private UUID senderAccountId;

    @Column(name = "recipient_account_id", nullable = false)
    private UUID recipientAccountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ContactRequestStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    protected ContactRequest() {
    }

    private ContactRequest(UUID senderAccountId, UUID recipientAccountId, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.senderAccountId = senderAccountId;
        this.recipientAccountId = recipientAccountId;
        this.status = ContactRequestStatus.PENDING;
        this.createdAt = createdAt;
    }

    public static ContactRequest create(UUID senderAccountId, UUID recipientAccountId, Instant createdAt) {
        return new ContactRequest(senderAccountId, recipientAccountId, createdAt);
    }

    public void accept(Instant respondedAt) {
        requirePending();
        this.status = ContactRequestStatus.ACCEPTED;
        this.respondedAt = respondedAt;
    }

    public void decline(Instant respondedAt) {
        requirePending();
        this.status = ContactRequestStatus.DECLINED;
        this.respondedAt = respondedAt;
    }

    private void requirePending() {
        if (status != ContactRequestStatus.PENDING) {
            throw new IllegalStateException("Contact request has already been resolved.");
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getSenderAccountId() {
        return senderAccountId;
    }

    public UUID getRecipientAccountId() {
        return recipientAccountId;
    }

    public ContactRequestStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }
}
