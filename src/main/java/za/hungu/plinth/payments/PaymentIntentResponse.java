package za.hungu.plinth.payments;

import java.time.Instant;
import java.util.UUID;

public record PaymentIntentResponse(
        UUID id,
        long amountMinor,
        String currency,
        PaymentIntentStatus status,
        String providerReference,
        Instant createdAt,
        Instant authorizedAt,
        Instant cancelledAt,
        boolean sandbox
) {
}
