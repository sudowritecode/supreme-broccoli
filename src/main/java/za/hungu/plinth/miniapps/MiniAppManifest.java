package za.hungu.plinth.miniapps;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "mini_app_manifests")
public class MiniAppManifest {
    @Id
    private UUID id;

    @Column(name = "app_id", nullable = false, length = 64)
    private String appId;

    @Column(name = "app_version", nullable = false, length = 32)
    private String appVersion;

    @Column(nullable = false, length = 120)
    private String issuer;

    @Column(nullable = false, length = 255)
    private String origin;

    @Column(name = "public_key_base64", nullable = false, length = 1024)
    private String publicKeyBase64;

    @Column(name = "signature_base64", nullable = false, length = 1024)
    private String signatureBase64;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "mini_app_manifest_permissions", joinColumns = @JoinColumn(name = "manifest_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false, length = 40)
    private Set<MiniAppPermission> permissions = EnumSet.noneOf(MiniAppPermission.class);

    protected MiniAppManifest() {
    }

    private MiniAppManifest(
            UUID id,
            String appId,
            String appVersion,
            String issuer,
            String origin,
            String publicKeyBase64,
            String signatureBase64,
            Set<MiniAppPermission> permissions,
            Instant createdAt
    ) {
        this.id = id;
        this.appId = appId;
        this.appVersion = appVersion;
        this.issuer = issuer;
        this.origin = origin;
        this.publicKeyBase64 = publicKeyBase64;
        this.signatureBase64 = signatureBase64;
        this.permissions = permissions.isEmpty() ? EnumSet.noneOf(MiniAppPermission.class) : EnumSet.copyOf(permissions);
        this.createdAt = createdAt;
    }

    public static MiniAppManifest register(
            String appId,
            String appVersion,
            String issuer,
            String origin,
            String publicKeyBase64,
            String signatureBase64,
            Set<MiniAppPermission> permissions,
            Instant createdAt
    ) {
        return new MiniAppManifest(
                UUID.randomUUID(), appId, appVersion, issuer, origin, publicKeyBase64, signatureBase64, permissions, createdAt
        );
    }

    public UUID getId() { return id; }
    public String getAppId() { return appId; }
    public String getAppVersion() { return appVersion; }
    public String getIssuer() { return issuer; }
    public String getOrigin() { return origin; }
    public String getPublicKeyBase64() { return publicKeyBase64; }
    public String getSignatureBase64() { return signatureBase64; }
    public Set<MiniAppPermission> getPermissions() { return permissions.isEmpty() ? Set.of() : Set.copyOf(permissions); }
    public Instant getCreatedAt() { return createdAt; }
}
