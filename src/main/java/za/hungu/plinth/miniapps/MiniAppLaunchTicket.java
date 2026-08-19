package za.hungu.plinth.miniapps;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mini_app_launch_tickets")
public class MiniAppLaunchTicket {
    @Id
    private UUID id;

    @Column(name = "manifest_id", nullable = false)
    private UUID manifestId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Column(nullable = false, unique = true, length = 64)
    private String nonce;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "ticket_signature_base64", nullable = false, length = 1024)
    private String ticketSignatureBase64;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MiniAppLaunchTicket() {
    }

    private MiniAppLaunchTicket(
            UUID id,
            UUID manifestId,
            UUID accountId,
            UUID deviceId,
            String nonce,
            Instant expiresAt,
            String ticketSignatureBase64,
            Instant createdAt
    ) {
        this.id = id;
        this.manifestId = manifestId;
        this.accountId = accountId;
        this.deviceId = deviceId;
        this.nonce = nonce;
        this.expiresAt = expiresAt;
        this.ticketSignatureBase64 = ticketSignatureBase64;
        this.createdAt = createdAt;
    }

    public static MiniAppLaunchTicket issue(
            UUID id,
            UUID manifestId,
            UUID accountId,
            UUID deviceId,
            String nonce,
            Instant expiresAt,
            String ticketSignatureBase64,
            Instant createdAt
    ) {
        return new MiniAppLaunchTicket(
                id, manifestId, accountId, deviceId, nonce, expiresAt, ticketSignatureBase64, createdAt
        );
    }

    public void consume(Instant consumedAt) {
        if (this.consumedAt != null) {
            throw new IllegalStateException("Launch ticket has already been consumed.");
        }
        if (!expiresAt.isAfter(consumedAt)) {
            throw new IllegalStateException("Launch ticket has expired.");
        }
        this.consumedAt = consumedAt;
    }

    public UUID getId() { return id; }
    public UUID getManifestId() { return manifestId; }
    public UUID getAccountId() { return accountId; }
    public UUID getDeviceId() { return deviceId; }
    public String getNonce() { return nonce; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getConsumedAt() { return consumedAt; }
    public String getTicketSignatureBase64() { return ticketSignatureBase64; }
    public Instant getCreatedAt() { return createdAt; }
}
