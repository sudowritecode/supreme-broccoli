package za.hungu.plinth.signal;

import java.time.Instant;
import java.util.UUID;

public record SignalIdentityVerificationResponse(
        UUID verifierDeviceId,
        UUID subjectDeviceId,
        String safetyNumberFingerprint,
        SignalIdentityVerificationStatus status,
        Instant verifiedAt,
        Instant changedAt,
        Instant updatedAt
) {
}
