package za.hungu.plinth.rooms;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "room_reports")
public class RoomReport {
    @Id
    private UUID id;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "reporter_account_id", nullable = false)
    private UUID reporterAccountId;

    @Column(name = "reported_account_id")
    private UUID reportedAccountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RoomReportReason reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RoomReport() {
    }

    private RoomReport(UUID id, UUID roomId, UUID reporterAccountId, UUID reportedAccountId, RoomReportReason reason, Instant createdAt) {
        this.id = id;
        this.roomId = roomId;
        this.reporterAccountId = reporterAccountId;
        this.reportedAccountId = reportedAccountId;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public static RoomReport create(UUID roomId, UUID reporterAccountId, UUID reportedAccountId, RoomReportReason reason, Instant createdAt) {
        return new RoomReport(UUID.randomUUID(), roomId, reporterAccountId, reportedAccountId, reason, createdAt);
    }

    public UUID getId() { return id; }
    public UUID getRoomId() { return roomId; }
    public UUID getReporterAccountId() { return reporterAccountId; }
    public UUID getReportedAccountId() { return reportedAccountId; }
    public RoomReportReason getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
}
