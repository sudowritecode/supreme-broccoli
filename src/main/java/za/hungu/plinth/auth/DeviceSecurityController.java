package za.hungu.plinth.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/devices")
public class DeviceSecurityController {

    private final DeviceAuthenticator deviceAuthenticator;
    private final DeviceSessionService deviceSessionService;
    private final DeviceRevocationService deviceRevocationService;

    public DeviceSecurityController(
            DeviceAuthenticator deviceAuthenticator,
            DeviceSessionService deviceSessionService,
            DeviceRevocationService deviceRevocationService
    ) {
        this.deviceAuthenticator = deviceAuthenticator;
        this.deviceSessionService = deviceSessionService;
        this.deviceRevocationService = deviceRevocationService;
    }

    @PostMapping("/{deviceId}/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse issueSession(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @PathVariable UUID deviceId
    ) {
        AuthenticatedDevice authenticatedDevice = deviceAuthenticator.require(deviceToken);
        if (!authenticatedDevice.deviceId().equals(deviceId)) {
            throw new AuthenticationRequiredException("A device can issue a session only for itself.");
        }
        DeviceSessionService.IssuedSession session = deviceSessionService.issue(deviceId);
        return new SessionResponse(session.sessionId(), session.token(), session.expiresAt());
    }

    @DeleteMapping("/{deviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeDevice(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @PathVariable UUID deviceId
    ) {
        deviceRevocationService.revokeOwnedDevice(deviceAuthenticator.require(deviceToken), deviceId);
    }

    public record SessionResponse(UUID sessionId, String sessionToken, Instant expiresAt) {
    }
}
