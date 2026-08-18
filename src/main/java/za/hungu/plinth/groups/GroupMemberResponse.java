package za.hungu.plinth.groups;

import za.hungu.plinth.conversation.ConversationMemberRole;
import za.hungu.plinth.conversation.ConversationMemberStatus;

import java.util.UUID;

public record GroupMemberResponse(
        UUID conversationId,
        UUID accountId,
        ConversationMemberRole role,
        ConversationMemberStatus status,
        long membershipVersion
) {
}
