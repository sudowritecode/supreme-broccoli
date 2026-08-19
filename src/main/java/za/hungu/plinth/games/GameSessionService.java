package za.hungu.plinth.games;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import za.hungu.plinth.auth.AuthenticatedDevice;
import za.hungu.plinth.conversation.Conversation;
import za.hungu.plinth.conversation.ConversationMemberRepository;
import za.hungu.plinth.conversation.ConversationMemberStatus;
import za.hungu.plinth.conversation.ConversationRepository;
import za.hungu.plinth.rooms.Room;
import za.hungu.plinth.rooms.RoomParticipant;
import za.hungu.plinth.rooms.RoomParticipantRepository;
import za.hungu.plinth.rooms.RoomParticipantRole;
import za.hungu.plinth.rooms.RoomParticipantStatus;
import za.hungu.plinth.rooms.RoomRepository;
import za.hungu.plinth.rooms.RoomStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class GameSessionService {
    private final CuratedGameCatalog curatedGameCatalog;
    private final GameSessionRepository gameSessionRepository;
    private final GameSessionParticipantRepository gameSessionParticipantRepository;
    private final RoomRepository roomRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;

    public GameSessionService(
            CuratedGameCatalog curatedGameCatalog,
            GameSessionRepository gameSessionRepository,
            GameSessionParticipantRepository gameSessionParticipantRepository,
            RoomRepository roomRepository,
            RoomParticipantRepository roomParticipantRepository,
            ConversationRepository conversationRepository,
            ConversationMemberRepository conversationMemberRepository
    ) {
        this.curatedGameCatalog = curatedGameCatalog;
        this.gameSessionRepository = gameSessionRepository;
        this.gameSessionParticipantRepository = gameSessionParticipantRepository;
        this.roomRepository = roomRepository;
        this.roomParticipantRepository = roomParticipantRepository;
        this.conversationRepository = conversationRepository;
        this.conversationMemberRepository = conversationMemberRepository;
    }

    @Transactional(readOnly = true)
    public List<CuratedGameDefinition> catalog() {
        return curatedGameCatalog.list();
    }

    @Transactional
    public GameSessionResponse startForRoom(AuthenticatedDevice caller, UUID roomId, StartGameSessionRequest request) {
        requireRoomModerator(roomId, caller.accountId());
        return start(caller, GameSessionSourceType.ROOM, roomId, request.gameId());
    }

    @Transactional
    public GameSessionResponse startForConversation(AuthenticatedDevice caller, UUID conversationId, StartGameSessionRequest request) {
        requireActiveConversationMember(conversationId, caller.accountId());
        return start(caller, GameSessionSourceType.CONVERSATION, conversationId, request.gameId());
    }

    @Transactional
    public GameSessionParticipantResponse join(AuthenticatedDevice caller, UUID gameSessionId) {
        GameSession session = requireActiveSession(gameSessionId);
        requireSourceAccess(session, caller.accountId());
        if (gameSessionParticipantRepository.findByGameSessionIdAndAccountId(gameSessionId, caller.accountId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The account already has game-session participation state.");
        }
        GameSessionParticipant participant = gameSessionParticipantRepository.save(
                GameSessionParticipant.join(gameSessionId, caller.accountId(), Instant.now())
        );
        return toParticipantResponse(participant);
    }

    @Transactional
    public GameSessionParticipantResponse leave(AuthenticatedDevice caller, UUID gameSessionId) {
        GameSession session = requireActiveSession(gameSessionId);
        GameSessionParticipant participant = gameSessionParticipantRepository.findByGameSessionIdAndAccountId(gameSessionId, caller.accountId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game participant was not found."));
        if (participant.getStatus() != GameSessionParticipantStatus.JOINED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The participant is no longer active in this game session.");
        }
        participant.leave(Instant.now());
        return toParticipantResponse(participant);
    }

    @Transactional
    public GameSessionResponse end(AuthenticatedDevice caller, UUID gameSessionId) {
        GameSession session = requireActiveSession(gameSessionId);
        boolean starter = session.getStartedByAccountId().equals(caller.accountId());
        boolean roomModerator = session.getSourceType() == GameSessionSourceType.ROOM && isRoomModerator(session.getSourceId(), caller.accountId());
        if (!starter && !roomModerator) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the session starter or a current room moderator can end this game session.");
        }
        session.end(Instant.now());
        return toResponse(session);
    }

    private GameSessionResponse start(AuthenticatedDevice caller, GameSessionSourceType sourceType, UUID sourceId, CuratedGameId gameId) {
        curatedGameCatalog.require(gameId);
        gameSessionRepository.findBySourceTypeAndSourceIdAndStatus(sourceType, sourceId, GameSessionStatus.ACTIVE)
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "An active game session already exists for this private context.");
                });
        GameSession session = gameSessionRepository.save(GameSession.start(
                gameId, sourceType, sourceId, caller.accountId(), caller.deviceId(), Instant.now()
        ));
        gameSessionParticipantRepository.save(GameSessionParticipant.join(session.getId(), caller.accountId(), session.getStartedAt()));
        return toResponse(session);
    }

    private GameSession requireActiveSession(UUID gameSessionId) {
        GameSession session = gameSessionRepository.findById(gameSessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game session was not found."));
        if (session.getStatus() != GameSessionStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game session is no longer active.");
        }
        return session;
    }

    private void requireSourceAccess(GameSession session, UUID accountId) {
        if (session.getSourceType() == GameSessionSourceType.ROOM) {
            requireActiveRoom(session.getSourceId());
            requireAdmittedRoomParticipant(session.getSourceId(), accountId);
        } else {
            requireActiveConversationMember(session.getSourceId(), accountId);
        }
    }

    private RoomParticipant requireRoomModerator(UUID roomId, UUID accountId) {
        Room room = requireActiveRoom(roomId);
        RoomParticipant participant = requireAdmittedRoomParticipant(room.getId(), accountId);
        if (participant.getRole() != RoomParticipantRole.HOST && participant.getRole() != RoomParticipantRole.CO_HOST) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Host or co-host access is required to start a room game.");
        }
        return participant;
    }

    private boolean isRoomModerator(UUID roomId, UUID accountId) {
        try {
            Room room = requireActiveRoom(roomId);
            RoomParticipant participant = requireAdmittedRoomParticipant(room.getId(), accountId);
            return participant.getRole() == RoomParticipantRole.HOST || participant.getRole() == RoomParticipantRole.CO_HOST;
        } catch (ResponseStatusException exception) {
            return false;
        }
    }

    private Room requireActiveRoom(UUID roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room was not found."));
        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room is no longer active.");
        }
        return room;
    }

    private RoomParticipant requireAdmittedRoomParticipant(UUID roomId, UUID accountId) {
        RoomParticipant participant = roomParticipantRepository.findByRoomIdAndAccountId(roomId, accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "The account is not an admitted room participant."));
        if (participant.getStatus() != RoomParticipantStatus.ADMITTED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The account is not an admitted room participant.");
        }
        return participant;
    }

    private Conversation requireActiveConversationMember(UUID conversationId, UUID accountId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation was not found."));
        if (!conversationMemberRepository.existsByConversationIdAndAccountIdAndMemberStatus(
                conversationId, accountId, ConversationMemberStatus.ACTIVE
        )) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The account is not an active conversation member.");
        }
        return conversation;
    }

    private GameSessionResponse toResponse(GameSession session) {
        return new GameSessionResponse(
                session.getId(), session.getGameId(), session.getSourceType(), session.getSourceId(), session.getStatus(),
                session.getStartedByAccountId(), session.getStartedAt(), session.getEndedAt(), false
        );
    }

    private GameSessionParticipantResponse toParticipantResponse(GameSessionParticipant participant) {
        return new GameSessionParticipantResponse(
                participant.getGameSessionId(), participant.getAccountId(), participant.getStatus(),
                participant.getJoinedAt(), participant.getLeftAt()
        );
    }
}
