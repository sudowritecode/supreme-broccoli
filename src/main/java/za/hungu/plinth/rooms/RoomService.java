package za.hungu.plinth.rooms;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import za.hungu.plinth.auth.AuthenticatedDevice;
import za.hungu.plinth.contacts.ContactRequestRepository;
import za.hungu.plinth.contacts.ContactRequestStatus;
import za.hungu.plinth.identity.AccountRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RoomService {
    private final RoomRepository roomRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final RoomBlockRepository roomBlockRepository;
    private final RoomReportRepository roomReportRepository;
    private final AccountInterestPreferenceRepository accountInterestPreferenceRepository;
    private final AccountRepository accountRepository;
    private final ContactRequestRepository contactRequestRepository;
    private final RoomMatchingService roomMatchingService;

    public RoomService(
            RoomRepository roomRepository,
            RoomParticipantRepository roomParticipantRepository,
            RoomBlockRepository roomBlockRepository,
            RoomReportRepository roomReportRepository,
            AccountInterestPreferenceRepository accountInterestPreferenceRepository,
            AccountRepository accountRepository,
            ContactRequestRepository contactRequestRepository,
            RoomMatchingService roomMatchingService
    ) {
        this.roomRepository = roomRepository;
        this.roomParticipantRepository = roomParticipantRepository;
        this.roomBlockRepository = roomBlockRepository;
        this.roomReportRepository = roomReportRepository;
        this.accountInterestPreferenceRepository = accountInterestPreferenceRepository;
        this.accountRepository = accountRepository;
        this.contactRequestRepository = contactRequestRepository;
        this.roomMatchingService = roomMatchingService;
    }

    @Transactional
    public RoomResponse create(AuthenticatedDevice caller, CreateRoomRequest request) {
        Set<String> tags = normalizeTags(request.interestTags());
        Room room = roomRepository.save(Room.create(
                normalizeTopic(request.topic()), request.capacity(), caller.accountId(), caller.deviceId(), tags, Instant.now()
        ));
        roomParticipantRepository.save(RoomParticipant.host(room.getId(), caller.accountId(), room.getCreatedAt()));
        return toRoomResponse(room);
    }

    @Transactional
    public List<String> updateInterestPreferences(AuthenticatedDevice caller, UpdateInterestPreferencesRequest request) {
        Set<String> desired = normalizeTags(request.interestTags());
        List<AccountInterestPreference> existing = accountInterestPreferenceRepository.findByAccountId(caller.accountId());
        Set<String> existingTags = existing.stream().map(AccountInterestPreference::getTag).collect(Collectors.toSet());
        Set<String> toRemove = new LinkedHashSet<>(existingTags);
        toRemove.removeAll(desired);
        if (!toRemove.isEmpty()) {
            accountInterestPreferenceRepository.deleteByAccountIdAndTagIn(caller.accountId(), toRemove);
        }
        desired.stream()
                .filter(tag -> !existingTags.contains(tag))
                .map(tag -> AccountInterestPreference.create(caller.accountId(), tag, Instant.now()))
                .forEach(accountInterestPreferenceRepository::save);
        return desired.stream().sorted().toList();
    }

    @Transactional
    public RoomParticipantResponse invite(AuthenticatedDevice caller, UUID roomId, RoomInvitationRequest request) {
        Room room = requireActiveRoom(roomId);
        requireModerator(roomId, caller.accountId());
        requireAccount(request.accountId());
        if (roomBlockRepository.existsByRoomIdAndBlockedAccountId(roomId, request.accountId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Blocked accounts cannot be invited to this room.");
        }
        if (!isAcceptedContact(caller.accountId(), request.accountId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Room invitations require an accepted contact relationship.");
        }
        if (roomParticipantRepository.findByRoomIdAndAccountId(roomId, request.accountId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The account already has room participation state.");
        }
        RoomParticipant participant = roomParticipantRepository.save(
                RoomParticipant.lobby(room.getId(), request.accountId(), caller.accountId(), Instant.now())
        );
        return toParticipantResponse(participant);
    }

    @Transactional
    public RoomParticipantResponse requestEntry(AuthenticatedDevice caller, UUID roomId) {
        Room room = requireActiveRoom(roomId);
        if (roomBlockRepository.existsByRoomIdAndBlockedAccountId(roomId, caller.accountId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is blocked from the room.");
        }
        if (roomParticipantRepository.findByRoomIdAndAccountId(roomId, caller.accountId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The account already has room participation state.");
        }
        Set<String> accountTags = accountInterestPreferenceRepository.findByAccountId(caller.accountId()).stream()
                .map(AccountInterestPreference::getTag)
                .collect(Collectors.toSet());
        if (!roomMatchingService.hasSharedInterest(room.getInterestTags(), accountTags)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Room entry requests require a shared opted-in interest.");
        }
        RoomParticipant participant = roomParticipantRepository.save(
                RoomParticipant.lobby(roomId, caller.accountId(), null, Instant.now())
        );
        return toParticipantResponse(participant);
    }

    @Transactional
    public RoomParticipantResponse promoteToCoHost(AuthenticatedDevice caller, UUID roomId, UUID accountId) {
        requireActiveRoom(roomId);
        RoomParticipant actor = requireParticipant(roomId, caller.accountId());
        if (actor.getStatus() != RoomParticipantStatus.ADMITTED || actor.getRole() != RoomParticipantRole.HOST) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the host can appoint a co-host.");
        }
        RoomParticipant participant = requireParticipant(roomId, accountId);
        if (participant.getRole() != RoomParticipantRole.PARTICIPANT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only a standard participant can be appointed co-host.");
        }
        try {
            participant.promoteToCoHost();
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
        return toParticipantResponse(participant);
    }

    @Transactional
    public RoomParticipantResponse admit(AuthenticatedDevice caller, UUID roomId, UUID accountId) {
        Room room = requireActiveRoom(roomId);
        requireModerator(roomId, caller.accountId());
        if (roomBlockRepository.existsByRoomIdAndBlockedAccountId(roomId, accountId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Blocked accounts cannot be admitted to this room.");
        }
        RoomParticipant participant = requireParticipant(roomId, accountId);
        if (participant.getStatus() != RoomParticipantStatus.LOBBY) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only lobby participants can be admitted.");
        }
        if (roomParticipantRepository.countByRoomIdAndStatus(roomId, RoomParticipantStatus.ADMITTED) >= room.getCapacity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The room has reached its capacity.");
        }
        participant.admit(Instant.now());
        return toParticipantResponse(participant);
    }

    @Transactional
    public RoomParticipantResponse leave(AuthenticatedDevice caller, UUID roomId) {
        requireActiveRoom(roomId);
        RoomParticipant participant = requireParticipant(roomId, caller.accountId());
        if (participant.getRole() == RoomParticipantRole.HOST) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The host must end the room rather than leave it.");
        }
        if (participant.getStatus() != RoomParticipantStatus.LOBBY && participant.getStatus() != RoomParticipantStatus.ADMITTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This participant is no longer active in the room.");
        }
        participant.leave(Instant.now());
        return toParticipantResponse(participant);
    }

    @Transactional
    public RoomParticipantResponse remove(AuthenticatedDevice caller, UUID roomId, UUID accountId) {
        requireActiveRoom(roomId);
        RoomParticipant actor = requireModerator(roomId, caller.accountId());
        RoomParticipant target = requireParticipant(roomId, accountId);
        if (target.getRole() == RoomParticipantRole.HOST) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The room host cannot be removed.");
        }
        if (actor.getRole() == RoomParticipantRole.CO_HOST && target.getRole() == RoomParticipantRole.CO_HOST) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A co-host cannot remove another co-host.");
        }
        target.remove(Instant.now());
        return toParticipantResponse(target);
    }

    @Transactional
    public RoomResponse end(AuthenticatedDevice caller, UUID roomId) {
        Room room = requireActiveRoom(roomId);
        if (!room.getHostAccountId().equals(caller.accountId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the host can end this room.");
        }
        room.end(Instant.now());
        return toRoomResponse(room);
    }

    @Transactional
    public void block(AuthenticatedDevice caller, UUID roomId, RoomBlockRequest request) {
        requireActiveRoom(roomId);
        requireModerator(roomId, caller.accountId());
        if (request.accountId().equals(caller.accountId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A moderator cannot block themselves from their own room.");
        }
        requireAccount(request.accountId());
        roomBlockRepository.save(RoomBlock.create(roomId, request.accountId(), caller.accountId(), Instant.now()));
        roomParticipantRepository.findByRoomIdAndAccountId(roomId, request.accountId()).ifPresent(participant -> {
            if (participant.getRole() != RoomParticipantRole.HOST) {
                participant.remove(Instant.now());
            }
        });
    }

    @Transactional
    public UUID report(AuthenticatedDevice caller, UUID roomId, CreateRoomReportRequest request) {
        requireActiveRoom(roomId);
        if (!roomParticipantRepository.existsByRoomIdAndAccountIdAndStatus(roomId, caller.accountId(), RoomParticipantStatus.ADMITTED)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admitted room participants can file room reports.");
        }
        if (request.reportedAccountId() != null) {
            requireAccount(request.reportedAccountId());
        }
        return roomReportRepository.save(RoomReport.create(
                roomId, caller.accountId(), request.reportedAccountId(), request.reason(), Instant.now()
        )).getId();
    }

    @Transactional(readOnly = true)
    public List<RoomSuggestionResponse> suggestions(AuthenticatedDevice caller) {
        return roomMatchingService.suggest(caller.accountId());
    }

    private Room requireActiveRoom(UUID roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room was not found."));
        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room is no longer active.");
        }
        return room;
    }

    private RoomParticipant requireModerator(UUID roomId, UUID accountId) {
        RoomParticipant participant = requireParticipant(roomId, accountId);
        if (participant.getStatus() != RoomParticipantStatus.ADMITTED ||
                (participant.getRole() != RoomParticipantRole.HOST && participant.getRole() != RoomParticipantRole.CO_HOST)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Host or co-host access is required.");
        }
        return participant;
    }

    private RoomParticipant requireParticipant(UUID roomId, UUID accountId) {
        return roomParticipantRepository.findByRoomIdAndAccountId(roomId, accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room participant was not found."));
    }

    private void requireAccount(UUID accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account was not found.");
        }
    }

    private boolean isAcceptedContact(UUID firstAccountId, UUID secondAccountId) {
        return contactRequestRepository.existsBySenderAccountIdAndRecipientAccountIdAndStatus(
                firstAccountId, secondAccountId, ContactRequestStatus.ACCEPTED
        ) || contactRequestRepository.existsBySenderAccountIdAndRecipientAccountIdAndStatus(
                secondAccountId, firstAccountId, ContactRequestStatus.ACCEPTED
        );
    }

    private Set<String> normalizeTags(Collection<String> tags) {
        Set<String> normalized = tags.stream()
                .map(tag -> tag.trim().toLowerCase(java.util.Locale.ROOT))
                .filter(tag -> !tag.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one non-blank interest tag is required.");
        }
        return normalized;
    }

    private String normalizeTopic(String topic) {
        String normalized = topic.trim();
        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A room topic is required.");
        }
        return normalized;
    }

    private RoomResponse toRoomResponse(Room room) {
        return new RoomResponse(
                room.getId(), room.getTopic(), room.getCapacity(), room.getStatus(), room.getHostAccountId(),
                room.getInterestTags(), room.getCreatedAt(), room.getEndedAt()
        );
    }

    private RoomParticipantResponse toParticipantResponse(RoomParticipant participant) {
        return new RoomParticipantResponse(
                participant.getRoomId(), participant.getAccountId(), participant.getRole(), participant.getStatus(),
                participant.getInvitedByAccountId(), participant.getRequestedAt(), participant.getAdmittedAt(),
                participant.getLeftAt(), participant.getRemovedAt()
        );
    }
}
