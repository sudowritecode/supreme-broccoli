package za.hungu.plinth.messaging;

import java.time.Instant;
import java.util.UUID;

public record MessageIngressResult(UUID messageId, Instant receivedAt, boolean duplicate) {
}
