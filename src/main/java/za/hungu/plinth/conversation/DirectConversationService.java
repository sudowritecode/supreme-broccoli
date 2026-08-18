package za.hungu.plinth.conversation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class DirectConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;

    public DirectConversationService(
            ConversationRepository conversationRepository,
            ConversationMemberRepository conversationMemberRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.conversationMemberRepository = conversationMemberRepository;
    }

    @Transactional
    public Conversation findOrCreate(UUID firstAccountId, UUID secondAccountId, Instant now) {
        String directKey = Conversation.canonicalDirectKey(firstAccountId, secondAccountId);
        return conversationRepository.findByDirectKey(directKey)
                .orElseGet(() -> create(firstAccountId, secondAccountId, now));
    }

    private Conversation create(UUID firstAccountId, UUID secondAccountId, Instant now) {
        Conversation conversation = conversationRepository.save(Conversation.direct(firstAccountId, secondAccountId, now));
        conversationMemberRepository.save(ConversationMember.join(conversation.getId(), firstAccountId, now));
        conversationMemberRepository.save(ConversationMember.join(conversation.getId(), secondAccountId, now));
        return conversation;
    }
}
