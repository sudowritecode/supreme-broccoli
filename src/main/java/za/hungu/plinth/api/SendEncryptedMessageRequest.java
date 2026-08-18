package za.hungu.plinth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Transport contract for a client-encrypted message envelope.
 * The authenticated device token supplies sender identity; this request never accepts it from the caller.
 */
public record SendEncryptedMessageRequest(
        @NotNull UUID messageId,
        @NotNull UUID conversationId,
        @NotNull UUID recipientDeviceId,
        @NotBlank @Size(max = 1_000_000) String ciphertext
) {
}
