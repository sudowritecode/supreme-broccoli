package za.hungu.plinth.payments;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_intents")
public class PaymentIntent {
    @Id
    private UUID id;

    @Column(name = "owner_account_id", nullable = false)
    private UUID ownerAccountId;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentIntentStatus status;

    @Column(name = "provider_reference", nullable = false, length = 128)
    private String providerReference;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "authorized_at")
    private Instant authorizedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    protected PaymentIntent() {
    }

    private PaymentIntent(UUID id, UUID ownerAccountId, String idempotencyKey, long amountMinor, String currency, String providerReference, Instant createdAt) {
        this.id = id;
        this.ownerAccountId = ownerAccountId;
        this.idempotencyKey = idempotencyKey;
        this.amountMinor = amountMinor;
        this.currency = currency;
        this.status = PaymentIntentStatus.REQUIRES_AUTHORIZATION;
        this.providerReference = providerReference;
        this.createdAt = createdAt;
    }

    public static PaymentIntent create(
            UUID id,
            UUID ownerAccountId,
            String idempotencyKey,
            long amountMinor,
            String currency,
            String providerReference,
            Instant createdAt
    ) {
        return new PaymentIntent(id, ownerAccountId, idempotencyKey, amountMinor, currency, providerReference, createdAt);
    }

    public void authorize(Instant authorizedAt) {
        if (status != PaymentIntentStatus.REQUIRES_AUTHORIZATION) {
            throw new IllegalStateException("Only a pending sandbox intent can be authorized.");
        }
        status = PaymentIntentStatus.AUTHORIZED;
        this.authorizedAt = authorizedAt;
    }

    public void cancel(Instant cancelledAt) {
        if (status != PaymentIntentStatus.REQUIRES_AUTHORIZATION) {
            throw new IllegalStateException("Only a pending sandbox intent can be cancelled.");
        }
        status = PaymentIntentStatus.CANCELLED;
        this.cancelledAt = cancelledAt;
    }

    public UUID getId() { return id; }
    public UUID getOwnerAccountId() { return ownerAccountId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public long getAmountMinor() { return amountMinor; }
    public String getCurrency() { return currency; }
    public PaymentIntentStatus getStatus() { return status; }
    public String getProviderReference() { return providerReference; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getAuthorizedAt() { return authorizedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
}
