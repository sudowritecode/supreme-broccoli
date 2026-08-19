package za.hungu.plinth.miniapps;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import za.hungu.plinth.auth.DeviceAuthenticator;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/mini-apps")
public class MiniAppController {
    public static final String REGISTRATION_KEY_HEADER = "X-Mini-App-Registration-Key";

    private final DeviceAuthenticator deviceAuthenticator;
    private final MiniAppService miniAppService;

    public MiniAppController(DeviceAuthenticator deviceAuthenticator, MiniAppService miniAppService) {
        this.deviceAuthenticator = deviceAuthenticator;
        this.miniAppService = miniAppService;
    }

    @PostMapping("/manifests")
    @ResponseStatus(HttpStatus.CREATED)
    public MiniAppManifestResponse register(
            @RequestHeader(name = REGISTRATION_KEY_HEADER, required = false) String registrationKey,
            @Valid @RequestBody RegisterMiniAppManifestRequest request
    ) {
        return miniAppService.register(registrationKey, request);
    }

    @GetMapping("/{appId}/versions/{appVersion}/manifest")
    public MiniAppManifestResponse manifest(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @PathVariable String appId,
            @PathVariable String appVersion
    ) {
        deviceAuthenticator.require(deviceToken);
        return miniAppService.manifest(appId, appVersion);
    }

    @PostMapping("/{appId}/versions/{appVersion}/launch-tickets")
    @ResponseStatus(HttpStatus.CREATED)
    public MiniAppLaunchTicketResponse createLaunchTicket(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @PathVariable String appId,
            @PathVariable String appVersion,
            @Valid @RequestBody CreateMiniAppLaunchTicketRequest request
    ) {
        return miniAppService.createLaunchTicket(deviceAuthenticator.require(deviceToken), appId, appVersion, request);
    }

    @PostMapping("/launch-tickets/{ticketId}/consume")
    public MiniAppLaunchTicketResponse consumeLaunchTicket(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @PathVariable UUID ticketId
    ) {
        return miniAppService.consumeLaunchTicket(deviceAuthenticator.require(deviceToken), ticketId);
    }
}
