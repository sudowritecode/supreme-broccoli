package za.hungu.plinth.rooms;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RoomInvitationRequest(@NotNull UUID accountId) {
}
