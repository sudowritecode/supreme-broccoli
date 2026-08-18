package za.hungu.plinth.delivery;

import java.time.Instant;
import java.util.UUID;

public record EncryptedMessageDeliveryEnvelope(
        String type,
        UUID deliveryId,
        UUID messageId,
        UUID conversationId,
        UUID senderDeviceId,
        String ciphertext,
        Instant receivedAt
) {
    public static final String TYPE = "encrypted_message";
}
