package za.hungu.plinth.rooms;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateRoomReportRequest(
        UUID reportedAccountId,
        @NotNull RoomReportReason reason
) {
}
