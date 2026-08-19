package za.hungu.plinth.signal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "signal_kyber_prekeys")
public class SignalKyberPrekey {
    @Id
    private UUID id;

    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Column(name = "prekey_id", nullable = false)
    private long prekeyId;

    @Column(name = "public_key", nullable = false, length = 16384)
    private String publicKey;

    @Column(nullable = false, length = 4096)
    private String signature;

    @Column(name = "last_resort", nullable = false)
    private boolean lastResort;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    @Column(name = "claim_id")
    private UUID claimId;

    protected SignalKyberPrekey() {
    }

    private SignalKyberPrekey(
            UUID id,
            UUID deviceId,
            long prekeyId,
            String publicKey,
            String signature,
            boolean lastResort,
            Instant createdAt
    ) {
        this.id = id;
        this.deviceId = deviceId;
        this.prekeyId = prekeyId;
        this.publicKey = publicKey;
        this.signature = signature;
        this.lastResort = lastResort;
        this.createdAt = createdAt;
    }

    public static SignalKyberPrekey create(
            UUID deviceId,
            long prekeyId,
            String publicKey,
            String signature,
            boolean lastResort,
            Instant createdAt
    ) {
        return new SignalKyberPrekey(UUID.randomUUID(), deviceId, prekeyId, publicKey, signature, lastResort, createdAt);
    }

    public void claim(UUID claimId, Instant claimedAt) {
        if (lastResort) {
            return;
        }
        if (this.claimedAt != null || this.claimId != null) {
            throw new IllegalStateException("A one-time Kyber prekey may be allocated once only.");
        }
        this.claimId = claimId;
        this.claimedAt = claimedAt;
    }

    public UUID getId() { return id; }
    public UUID getDeviceId() { return deviceId; }
    public long getPrekeyId() { return prekeyId; }
    public String getPublicKey() { return publicKey; }
    public String getSignature() { return signature; }
    public boolean isLastResort() { return lastResort; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getClaimedAt() { return claimedAt; }
    public UUID getClaimId() { return claimId; }
}
