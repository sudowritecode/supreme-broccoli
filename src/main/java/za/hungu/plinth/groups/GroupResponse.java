package za.hungu.plinth.groups;

import za.hungu.plinth.conversation.ConversationMemberRole;
import za.hungu.plinth.conversation.ConversationMemberStatus;

import java.util.UUID;

public record GroupResponse(
        UUID conversationId,
        String name,
        long membershipVersion,
        ConversationMemberRole callerRole,
        ConversationMemberStatus callerStatus
) {
}
