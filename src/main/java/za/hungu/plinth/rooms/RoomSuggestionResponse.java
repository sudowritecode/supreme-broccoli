package za.hungu.plinth.rooms;

import java.util.UUID;

public record RoomSuggestionResponse(
        UUID roomId,
        String topic,
        int capacity,
        long admittedParticipantCount,
        RoomMatchReason reason
) {
}
