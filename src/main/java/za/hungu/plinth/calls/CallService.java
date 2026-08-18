package za.hungu.plinth.calls;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import za.hungu.plinth.auth.AuthenticatedDevice;
import za.hungu.plinth.conversation.Conversation;
import za.hungu.plinth.conversation.ConversationMemberRepository;
import za.hungu.plinth.conversation.ConversationMemberStatus;
import za.hungu.plinth.conversation.ConversationRepository;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class CallService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final CallSessionRepository callSessionRepository;
    private final CallParticipantRepository callParticipantRepository;

    public CallService(
            ConversationRepository conversationRepository,
            ConversationMemberRepository conversationMemberRepository,
            CallSessionRepository callSessionRepository,
            CallParticipantRepository callParticipantRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.conversationMemberRepository = conversationMemberRepository;
        this.callSessionRepository = callSessionRepository;
        this.callParticipantRepository = callParticipantRepository;
    }

    public CallResponse start(AuthenticatedDevice caller, UUID conversationId) {
        requireActiveConversationMember(conversationId, caller.accountId());
        CallSession existing = callSessionRepository
                .findFirstByConversationIdAndStatusOrderByStartedAtDesc(conversationId, CallSessionStatus.ACTIVE)
                .orElse(null);
        if (existing != null) {
            return toCallResponse(existing, caller.accountId());
        }
        CallSession session = callSessionRepository.save(CallSession.start(conversationId, caller.deviceId(), Instant.now()));
        callParticipantRepository.save(CallParticipant.join(session.getId(), caller.accountId(), Instant.now()));
        return toCallResponse(session, caller.accountId());
    }

    public CallParticipantResponse join(AuthenticatedDevice participant, UUID callSessionId) {
        CallSession session = requireActiveSession(callSessionId);
        requireActiveConversationMember(session.getConversationId(), participant.accountId());
        CallParticipant existing = callParticipantRepository.findByCallSessionIdAndAccountId(callSessionId, participant.accountId())
                .orElse(null);
        if (existing != null) {
            if (existing.getStatus() == CallParticipantStatus.ACTIVE) {
                return toParticipantResponse(existing);
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This account has already left or been removed from the call.");
        }
        CallParticipant joined = callParticipantRepository.save(CallParticipant.join(callSessionId, participant.accountId(), Instant.now()));
        return toParticipantResponse(joined);
    }

    public CallParticipantResponse leave(AuthenticatedDevice participant, UUID callSessionId) {
        requireActiveSession(callSessionId);
        CallParticipant callParticipant = callParticipantRepository.findByCallSessionIdAndAccountId(callSessionId, participant.accountId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Call participant was not found."));
        callParticipant.leave(Instant.now());
        return toParticipantResponse(callParticipant);
    }

    public CallResponse end(AuthenticatedDevice caller, UUID callSessionId) {
        CallSession session = requireActiveSession(callSessionId);
        if (!session.getStartedByDeviceId().equals(caller.deviceId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the device that started the call can end it.");
        }
        session.end(Instant.now());
        return toCallResponse(session, caller.accountId());
    }

    private CallSession requireActiveSession(UUID callSessionId) {
        CallSession session = callSessionRepository.findById(callSessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Call session was not found."));
        if (session.getStatus() != CallSessionStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Call session is no longer active.");
        }
        return session;
    }

    private Conversation requireActiveConversationMember(UUID conversationId, UUID accountId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation was not found."));
        boolean active = conversationMemberRepository.existsByConversationIdAndAccountIdAndMemberStatus(
                conversationId, accountId, ConversationMemberStatus.ACTIVE
        );
        if (!active) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The account is not an active conversation member.");
        }
        return conversation;
    }

    private CallResponse toCallResponse(CallSession session, UUID accountId) {
        CallParticipantStatus participantStatus = callParticipantRepository
                .findByCallSessionIdAndAccountId(session.getId(), accountId)
                .map(CallParticipant::getStatus)
                .orElse(null);
        return new CallResponse(
                session.getId(),
                session.getConversationId(),
                session.getStatus(),
                participantStatus,
                false,
                session.getStartedAt()
        );
    }

    private CallParticipantResponse toParticipantResponse(CallParticipant participant) {
        return new CallParticipantResponse(
                participant.getCallSessionId(),
                participant.getAccountId(),
                participant.getStatus(),
                false
        );
    }
}
