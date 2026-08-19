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
@Table(name = "signal_device_key_bundles")
public class SignalDeviceKeyBundle {
    @Id
    @Column(name = "device_id")
    private UUID deviceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "protocol_profile", nullable = false, length = 64)
    private SignalProtocolProfile protocolProfile;

    @Column(name = "protocol_device_id", nullable = false)
    private int protocolDeviceId;

    @Column(name = "registration_id", nullable = false)
    private int registrationId;

    @Column(name = "identity_key", nullable = false, length = 4096)
    private String identityKey;

    @Column(name = "signed_prekey_id", nullable = false)
    private long signedPrekeyId;

    @Column(name = "signed_prekey_public", nullable = false, length = 4096)
    private String signedPrekeyPublic;

    @Column(name = "signed_prekey_signature", nullable = false, length = 4096)
    private String signedPrekeySignature;

    @Column(name = "signed_prekey_created_at", nullable = false)
    private Instant signedPrekeyCreatedAt;

    @Column(name = "signed_prekey_expires_at", nullable = false)
    private Instant signedPrekeyExpiresAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SignalDeviceKeyBundle() {
    }

    private SignalDeviceKeyBundle(
            UUID deviceId,
            SignalProtocolProfile protocolProfile,
            int protocolDeviceId,
            int registrationId,
            String identityKey,
            long signedPrekeyId,
            String signedPrekeyPublic,
            String signedPrekeySignature,
            Instant signedPrekeyCreatedAt,
            Instant signedPrekeyExpiresAt,
            Instant updatedAt
    ) {
        this.deviceId = deviceId;
        this.protocolProfile = protocolProfile;
        this.protocolDeviceId = protocolDeviceId;
        this.registrationId = registrationId;
        this.identityKey = identityKey;
        this.signedPrekeyId = signedPrekeyId;
        this.signedPrekeyPublic = signedPrekeyPublic;
        this.signedPrekeySignature = signedPrekeySignature;
        this.signedPrekeyCreatedAt = signedPrekeyCreatedAt;
        this.signedPrekeyExpiresAt = signedPrekeyExpiresAt;
        this.updatedAt = updatedAt;
    }

    public static SignalDeviceKeyBundle register(
            UUID deviceId,
            SignalProtocolProfile protocolProfile,
            int protocolDeviceId,
            int registrationId,
            String identityKey,
            long signedPrekeyId,
            String signedPrekeyPublic,
            String signedPrekeySignature,
            Instant signedPrekeyCreatedAt,
            Instant signedPrekeyExpiresAt,
            Instant updatedAt
    ) {
        return new SignalDeviceKeyBundle(
                deviceId, protocolProfile, protocolDeviceId, registrationId, identityKey, signedPrekeyId,
                signedPrekeyPublic, signedPrekeySignature, signedPrekeyCreatedAt, signedPrekeyExpiresAt, updatedAt
        );
    }

    public void replace(
            SignalProtocolProfile protocolProfile,
            int protocolDeviceId,
            int registrationId,
            String identityKey,
            long signedPrekeyId,
            String signedPrekeyPublic,
            String signedPrekeySignature,
            Instant signedPrekeyCreatedAt,
            Instant signedPrekeyExpiresAt,
            Instant updatedAt
    ) {
        this.protocolProfile = protocolProfile;
        this.protocolDeviceId = protocolDeviceId;
        this.registrationId = registrationId;
        this.identityKey = identityKey;
        this.signedPrekeyId = signedPrekeyId;
        this.signedPrekeyPublic = signedPrekeyPublic;
        this.signedPrekeySignature = signedPrekeySignature;
        this.signedPrekeyCreatedAt = signedPrekeyCreatedAt;
        this.signedPrekeyExpiresAt = signedPrekeyExpiresAt;
        this.updatedAt = updatedAt;
    }

    public UUID getDeviceId() { return deviceId; }
    public SignalProtocolProfile getProtocolProfile() { return protocolProfile; }
    public int getProtocolDeviceId() { return protocolDeviceId; }
    public int getRegistrationId() { return registrationId; }
    public String getIdentityKey() { return identityKey; }
    public long getSignedPrekeyId() { return signedPrekeyId; }
    public String getSignedPrekeyPublic() { return signedPrekeyPublic; }
    public String getSignedPrekeySignature() { return signedPrekeySignature; }
    public Instant getSignedPrekeyCreatedAt() { return signedPrekeyCreatedAt; }
    public Instant getSignedPrekeyExpiresAt() { return signedPrekeyExpiresAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
