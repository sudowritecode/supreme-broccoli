package za.hungu.plinth.auth;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import za.hungu.plinth.identity.Device;
import za.hungu.plinth.identity.DeviceRepository;

import java.time.Instant;

@Component
public class DeviceAuthenticator {

    public static final String HEADER_NAME = "X-Device-Token";

    private final DeviceRepository deviceRepository;
    private final DeviceSessionRepository deviceSessionRepository;
    private final DeviceTokenService deviceTokenService;

    public DeviceAuthenticator(
            DeviceRepository deviceRepository,
            DeviceSessionRepository deviceSessionRepository,
            DeviceTokenService deviceTokenService
    ) {
        this.deviceRepository = deviceRepository;
        this.deviceSessionRepository = deviceSessionRepository;
        this.deviceTokenService = deviceTokenService;
    }

    @Transactional
    public AuthenticatedDevice require(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new AuthenticationRequiredException();
        }

        String tokenHash = deviceTokenService.hash(rawToken);
        Instant now = Instant.now();
        DeviceSession activeSession = deviceSessionRepository
                .findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(tokenHash, now)
                .orElse(null);

        if (activeSession != null) {
            Device device = deviceRepository.findByIdAndRevokedAtIsNull(activeSession.getDeviceId())
                    .orElseThrow(AuthenticationRequiredException::new);
            activeSession.recordSeenAt(now);
            return new AuthenticatedDevice(device.getId(), device.getAccountId());
        }

        Device compatibleDevelopmentDevice = deviceRepository
                .findByAccessTokenHashAndRevokedAtIsNull(tokenHash)
                .orElseThrow(AuthenticationRequiredException::new);
        return new AuthenticatedDevice(compatibleDevelopmentDevice.getId(), compatibleDevelopmentDevice.getAccountId());
    }
}
