package za.hungu.plinth.conversation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "conversation_members")
@IdClass(ConversationMember.ConversationMemberId.class)
public class ConversationMember {

    @Id
    @Column(name = "conversation_id")
    private UUID conversationId;

    @Id
    @Column(name = "account_id")
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ConversationMemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_status", nullable = false, length = 16)
    private ConversationMemberStatus memberStatus;

    @Column(name = "membership_version", nullable = false)
    private long membershipVersion;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    protected ConversationMember() {
    }

    private ConversationMember(
            UUID conversationId,
            UUID accountId,
            ConversationMemberRole role,
            ConversationMemberStatus memberStatus,
            long membershipVersion,
            Instant addedAt
    ) {
        this.conversationId = conversationId;
        this.accountId = accountId;
        this.role = role;
        this.memberStatus = memberStatus;
        this.membershipVersion = membershipVersion;
        this.addedAt = addedAt;
    }

    public static ConversationMember join(UUID conversationId, UUID accountId, Instant addedAt) {
        return active(conversationId, accountId, ConversationMemberRole.MEMBER, 0, addedAt);
    }

    public static ConversationMember active(
            UUID conversationId,
            UUID accountId,
            ConversationMemberRole role,
            long membershipVersion,
            Instant addedAt
    ) {
        return new ConversationMember(
                conversationId,
                accountId,
                role,
                ConversationMemberStatus.ACTIVE,
                membershipVersion,
                addedAt
        );
    }

    public static ConversationMember invited(
            UUID conversationId,
            UUID accountId,
            long membershipVersion,
            Instant invitedAt
    ) {
        return new ConversationMember(
                conversationId,
                accountId,
                ConversationMemberRole.MEMBER,
                ConversationMemberStatus.INVITED,
                membershipVersion,
                invitedAt
        );
    }

    public void accept(long version, Instant acceptedAt) {
        requireStatus(ConversationMemberStatus.INVITED);
        this.memberStatus = ConversationMemberStatus.ACTIVE;
        this.membershipVersion = version;
        this.addedAt = acceptedAt;
        this.leftAt = null;
    }

    public void decline(long version, Instant declinedAt) {
        requireStatus(ConversationMemberStatus.INVITED);
        this.memberStatus = ConversationMemberStatus.DECLINED;
        this.membershipVersion = version;
        this.leftAt = declinedAt;
    }

    public void leave(long version, Instant leftAt) {
        requireActive();
        this.memberStatus = ConversationMemberStatus.LEFT;
        this.membershipVersion = version;
        this.leftAt = leftAt;
    }

    public void remove(long version, Instant removedAt) {
        requireActive();
        this.memberStatus = ConversationMemberStatus.REMOVED;
        this.membershipVersion = version;
        this.leftAt = removedAt;
    }

    private void requireActive() {
        requireStatus(ConversationMemberStatus.ACTIVE);
    }

    private void requireStatus(ConversationMemberStatus expected) {
        if (memberStatus != expected) {
            throw new IllegalStateException("Conversation member is not in the required state.");
        }
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public ConversationMemberRole getRole() {
        return role;
    }

    public ConversationMemberStatus getMemberStatus() {
        return memberStatus;
    }

    public long getMembershipVersion() {
        return membershipVersion;
    }

    public boolean isActive() {
        return memberStatus == ConversationMemberStatus.ACTIVE;
    }

    public static final class ConversationMemberId implements Serializable {

        private UUID conversationId;
        private UUID accountId;

        public ConversationMemberId() {
        }

        public ConversationMemberId(UUID conversationId, UUID accountId) {
            this.conversationId = conversationId;
            this.accountId = accountId;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof ConversationMemberId that)) {
                return false;
            }
            return Objects.equals(conversationId, that.conversationId)
                    && Objects.equals(accountId, that.accountId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(conversationId, accountId);
        }
    }
}
