package za.hungu.plinth.rooms;

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
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "rooms")
public class Room {
    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String topic;

    @Column(nullable = false)
    private int capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RoomStatus status;

    @Column(name = "host_account_id", nullable = false)
    private UUID hostAccountId;

    @Column(name = "host_device_id", nullable = false)
    private UUID hostDeviceId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "room_interest_tags", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "tag", nullable = false, length = 32)
    private Set<String> interestTags = new LinkedHashSet<>();

    protected Room() {
    }

    private Room(UUID id, String topic, int capacity, UUID hostAccountId, UUID hostDeviceId, Set<String> interestTags, Instant createdAt) {
        this.id = id;
        this.topic = topic;
        this.capacity = capacity;
        this.status = RoomStatus.ACTIVE;
        this.hostAccountId = hostAccountId;
        this.hostDeviceId = hostDeviceId;
        this.interestTags = new LinkedHashSet<>(interestTags);
        this.createdAt = createdAt;
    }

    public static Room create(String topic, int capacity, UUID hostAccountId, UUID hostDeviceId, Set<String> interestTags, Instant createdAt) {
        return new Room(UUID.randomUUID(), topic, capacity, hostAccountId, hostDeviceId, interestTags, createdAt);
    }

    public void end(Instant endedAt) {
        if (status == RoomStatus.ACTIVE) {
            status = RoomStatus.ENDED;
            this.endedAt = endedAt;
        }
    }

    public UUID getId() { return id; }
    public String getTopic() { return topic; }
    public int getCapacity() { return capacity; }
    public RoomStatus getStatus() { return status; }
    public UUID getHostAccountId() { return hostAccountId; }
    public UUID getHostDeviceId() { return hostDeviceId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getEndedAt() { return endedAt; }
    public Set<String> getInterestTags() { return Set.copyOf(interestTags); }
}
