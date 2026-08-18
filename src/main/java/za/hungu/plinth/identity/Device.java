package za.hungu.plinth.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "devices")
public class Device {

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "public_identity_key", nullable = false, length = 4096)
    private String publicIdentityKey;

    @Column(name = "access_token_hash", nullable = false, unique = true, length = 64)
    private String accessTokenHash;

    @Column(nullable = false, length = 120)
    private String label;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected Device() {
    }

    private Device(
            UUID id,
            UUID accountId,
            String publicIdentityKey,
            String accessTokenHash,
            String label,
            Instant createdAt
    ) {
        this.id = id;
        this.accountId = accountId;
        this.publicIdentityKey = publicIdentityKey;
        this.accessTokenHash = accessTokenHash;
        this.label = label;
        this.createdAt = createdAt;
    }

    public static Device create(
            UUID accountId,
            String publicIdentityKey,
            String accessTokenHash,
            String label,
            Instant createdAt
    ) {
        return new Device(UUID.randomUUID(), accountId, publicIdentityKey, accessTokenHash, label, createdAt);
    }

    public void revoke(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getPublicIdentityKey() {
        return publicIdentityKey;
    }

    public String getLabel() {
        return label;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}
