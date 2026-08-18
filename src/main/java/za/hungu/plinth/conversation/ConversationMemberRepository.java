package za.hungu.plinth.conversation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, ConversationMember.ConversationMemberId> {

    boolean existsByConversationIdAndAccountIdAndLeftAtIsNull(UUID conversationId, UUID accountId);

    boolean existsByConversationIdAndAccountIdAndMemberStatus(
            UUID conversationId,
            UUID accountId,
            ConversationMemberStatus memberStatus
    );

    Optional<ConversationMember> findByConversationIdAndAccountId(UUID conversationId, UUID accountId);

    List<ConversationMember> findByConversationIdAndMemberStatusOrderByMembershipVersionAsc(
            UUID conversationId,
            ConversationMemberStatus memberStatus
    );
}
