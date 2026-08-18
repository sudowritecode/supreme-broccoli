package za.hungu.plinth.auth;

import java.util.UUID;

public record AuthenticatedDevice(UUID deviceId, UUID accountId) {
}
