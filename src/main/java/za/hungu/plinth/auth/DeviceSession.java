package za.hungu.plinth.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "device_sessions")
public class DeviceSession {

    @Id
    private UUID id;

    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    protected DeviceSession() {
    }

    private DeviceSession(UUID id, UUID deviceId, String tokenHash, Instant issuedAt, Instant expiresAt) {
        this.id = id;
        this.deviceId = deviceId;
        this.tokenHash = tokenHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    public static DeviceSession create(UUID deviceId, String tokenHash, Instant issuedAt, Instant expiresAt) {
        return new DeviceSession(UUID.randomUUID(), deviceId, tokenHash, issuedAt, expiresAt);
    }

    public boolean isActiveAt(Instant instant) {
        return revokedAt == null && expiresAt.isAfter(instant);
    }

    public void recordSeenAt(Instant instant) {
        this.lastSeenAt = instant;
    }

    public void revoke(Instant instant) {
        this.revokedAt = instant;
    }

    public UUID getId() {
        return id;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}
