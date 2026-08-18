package za.hungu.plinth.calls;

import java.time.Instant;
import java.util.UUID;

/**
 * Provider-neutral media boundary. Implementations must issue short-lived credentials only after
 * platform conversation and participant checks are complete.
 */
public interface CallMediaPort {

    MediaSession createSession(UUID callSessionId, Instant expiresAt);

    ParticipantCredential issueParticipantCredential(UUID callSessionId, UUID accountId, Instant expiresAt);

    void removeParticipant(UUID callSessionId, UUID accountId);

    void endSession(UUID callSessionId);

    record MediaSession(String providerSessionId, Instant expiresAt) {
    }

    record ParticipantCredential(String value, Instant expiresAt) {
    }
}
