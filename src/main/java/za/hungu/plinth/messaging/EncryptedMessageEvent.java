package za.hungu.plinth.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Broker payload for a client-encrypted message envelope.
 *
 * <p>The {@code ciphertext} field must already have been encrypted on the client.
 * The platform must never accept plaintext chat content in this event.</p>
 */
public record EncryptedMessageEvent(
        UUID messageId,
        UUID conversationId,
        UUID senderDeviceId,
        UUID recipientDeviceId,
        String ciphertext,
        Instant receivedAt
) {
}
