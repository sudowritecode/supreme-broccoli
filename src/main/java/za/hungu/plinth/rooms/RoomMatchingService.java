package za.hungu.plinth.rooms;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RoomMatchingService {
    private final RoomRepository roomRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final RoomBlockRepository roomBlockRepository;
    private final AccountInterestPreferenceRepository accountInterestPreferenceRepository;

    public RoomMatchingService(
            RoomRepository roomRepository,
            RoomParticipantRepository roomParticipantRepository,
            RoomBlockRepository roomBlockRepository,
            AccountInterestPreferenceRepository accountInterestPreferenceRepository
    ) {
        this.roomRepository = roomRepository;
        this.roomParticipantRepository = roomParticipantRepository;
        this.roomBlockRepository = roomBlockRepository;
        this.accountInterestPreferenceRepository = accountInterestPreferenceRepository;
    }

    @Transactional(readOnly = true)
    public List<RoomSuggestionResponse> suggest(UUID accountId) {
        Map<UUID, RoomSuggestionResponse> suggestions = new LinkedHashMap<>();

        roomParticipantRepository.findByAccountIdAndStatus(accountId, RoomParticipantStatus.LOBBY).stream()
                .map(participant -> roomRepository.findById(participant.getRoomId()).orElse(null))
                .filter(room -> room != null && room.getStatus() == RoomStatus.ACTIVE)
                .filter(room -> !roomBlockRepository.existsByRoomIdAndBlockedAccountId(room.getId(), accountId))
                .forEach(room -> suggestions.put(room.getId(), suggestion(room, RoomMatchReason.DIRECT_INVITE)));

        Set<String> accountTags = accountInterestPreferenceRepository.findByAccountId(accountId).stream()
                .map(AccountInterestPreference::getTag)
                .collect(Collectors.toUnmodifiableSet());
        if (!accountTags.isEmpty()) {
            roomRepository.findByStatusOrderByCreatedAtDesc(RoomStatus.ACTIVE).stream()
                    .filter(room -> !room.getHostAccountId().equals(accountId))
                    .filter(room -> !roomBlockRepository.existsByRoomIdAndBlockedAccountId(room.getId(), accountId))
                    .filter(room -> roomParticipantRepository.findByRoomIdAndAccountId(room.getId(), accountId).isEmpty())
                    .filter(room -> hasSharedInterest(room.getInterestTags(), accountTags))
                    .forEach(room -> suggestions.putIfAbsent(room.getId(), suggestion(room, RoomMatchReason.SHARED_INTEREST)));
        }

        return suggestions.values().stream()
                .sorted(Comparator.comparing(RoomSuggestionResponse::reason)
                        .thenComparing(RoomSuggestionResponse::admittedParticipantCount)
                        .thenComparing(RoomSuggestionResponse::roomId))
                .toList();
    }

    public boolean hasSharedInterest(Set<String> roomTags, Set<String> accountTags) {
        return roomTags.stream().anyMatch(accountTags::contains);
    }

    private RoomSuggestionResponse suggestion(Room room, RoomMatchReason reason) {
        return new RoomSuggestionResponse(
                room.getId(),
                room.getTopic(),
                room.getCapacity(),
                roomParticipantRepository.countByRoomIdAndStatus(room.getId(), RoomParticipantStatus.ADMITTED),
                reason
        );
    }
}
