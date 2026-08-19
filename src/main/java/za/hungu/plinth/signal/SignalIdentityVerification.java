package za.hungu.plinth.signal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "signal_identity_verifications")
public class SignalIdentityVerification {
    @Id
    private UUID id;

    @Column(name = "verifier_device_id", nullable = false)
    private UUID verifierDeviceId;

    @Column(name = "subject_device_id", nullable = false)
    private UUID subjectDeviceId;

    @Column(name = "safety_number_fingerprint", nullable = false, length = 512)
    private String safetyNumberFingerprint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SignalIdentityVerificationStatus status;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "changed_at")
    private Instant changedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SignalIdentityVerification() {
    }

    private SignalIdentityVerification(UUID verifierDeviceId, UUID subjectDeviceId, String fingerprint, Instant now) {
        this.id = UUID.randomUUID();
        this.verifierDeviceId = verifierDeviceId;
        this.subjectDeviceId = subjectDeviceId;
        this.safetyNumberFingerprint = fingerprint;
        this.status = SignalIdentityVerificationStatus.UNVERIFIED;
        this.updatedAt = now;
    }

    public static SignalIdentityVerification create(UUID verifierDeviceId, UUID subjectDeviceId, String fingerprint, Instant now) {
        return new SignalIdentityVerification(verifierDeviceId, subjectDeviceId, fingerprint, now);
    }

    public void verify(String fingerprint, Instant now) {
        this.safetyNumberFingerprint = fingerprint;
        this.status = SignalIdentityVerificationStatus.VERIFIED;
        this.verifiedAt = now;
        this.changedAt = null;
        this.updatedAt = now;
    }

    public void markChanged(String fingerprint, Instant now) {
        this.safetyNumberFingerprint = fingerprint;
        this.status = SignalIdentityVerificationStatus.CHANGED;
        this.verifiedAt = null;
        this.changedAt = now;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getVerifierDeviceId() { return verifierDeviceId; }
    public UUID getSubjectDeviceId() { return subjectDeviceId; }
    public String getSafetyNumberFingerprint() { return safetyNumberFingerprint; }
    public SignalIdentityVerificationStatus getStatus() { return status; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public Instant getChangedAt() { return changedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
