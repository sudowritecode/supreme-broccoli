package za.hungu.plinth.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class DeviceSessionService {

    private static final Duration DEFAULT_SESSION_LIFETIME = Duration.ofDays(30);

    private final DeviceSessionRepository deviceSessionRepository;
    private final DeviceTokenService deviceTokenService;

    public DeviceSessionService(DeviceSessionRepository deviceSessionRepository, DeviceTokenService deviceTokenService) {
        this.deviceSessionRepository = deviceSessionRepository;
        this.deviceTokenService = deviceTokenService;
    }

    @Transactional
    public IssuedSession issue(UUID deviceId) {
        Instant issuedAt = Instant.now();
        DeviceTokenService.IssuedToken token = deviceTokenService.issue();
        DeviceSession session = DeviceSession.create(
                deviceId,
                token.hash(),
                issuedAt,
                issuedAt.plus(DEFAULT_SESSION_LIFETIME)
        );
        deviceSessionRepository.save(session);
        return new IssuedSession(session.getId(), token.rawValue(), session.getExpiresAt());
    }

    @Transactional
    public DeviceSession resolveActive(String rawToken) {
        Instant now = Instant.now();
        DeviceSession session = deviceSessionRepository
                .findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(deviceTokenService.hash(rawToken), now)
                .orElseThrow(() -> new AuthenticationRequiredException("A valid active device session is required."));
        session.recordSeenAt(now);
        return session;
    }

    @Transactional
    public void revokeForDevice(UUID deviceId) {
        Instant now = Instant.now();
        deviceSessionRepository.findByDeviceIdAndRevokedAtIsNull(deviceId)
                .forEach(session -> session.revoke(now));
    }

    public record IssuedSession(UUID sessionId, String token, Instant expiresAt) {
    }
}
