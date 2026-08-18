package za.hungu.plinth.outbox;

import java.util.UUID;

public record EncryptedMessageQueued(UUID outboxId) {
}
