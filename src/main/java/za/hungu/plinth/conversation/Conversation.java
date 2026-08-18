package za.hungu.plinth.conversation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    private UUID id;

    @Column(name = "direct_key", unique = true, length = 73)
    private String directKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "conversation_type", nullable = false, length = 16)
    private ConversationType type;

    @Column(length = 120)
    private String name;

    @Column(name = "membership_version", nullable = false)
    private long membershipVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Conversation() {
    }

    private Conversation(
            String directKey,
            ConversationType type,
            String name,
            Instant createdAt
    ) {
        this.id = UUID.randomUUID();
        this.directKey = directKey;
        this.type = type;
        this.name = name;
        this.membershipVersion = 0;
        this.createdAt = createdAt;
    }

    public static Conversation direct(UUID firstAccountId, UUID secondAccountId, Instant createdAt) {
        return new Conversation(canonicalDirectKey(firstAccountId, secondAccountId), ConversationType.DIRECT, null, createdAt);
    }

    public static Conversation group(String name, Instant createdAt) {
        return new Conversation(null, ConversationType.GROUP, name, createdAt);
    }

    public static String canonicalDirectKey(UUID firstAccountId, UUID secondAccountId) {
        String first = firstAccountId.toString();
        String second = secondAccountId.toString();
        return first.compareTo(second) < 0 ? first + ":" + second : second + ":" + first;
    }

    public long incrementMembershipVersion() {
        membershipVersion++;
        return membershipVersion;
    }

    public UUID getId() {
        return id;
    }

    public String getDirectKey() {
        return directKey;
    }

    public ConversationType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public long getMembershipVersion() {
        return membershipVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
