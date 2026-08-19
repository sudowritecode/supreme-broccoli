package za.hungu.plinth.signal;

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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/signal")
public class SignalProtocolController {
    private final DeviceAuthenticator deviceAuthenticator;
    private final SignalProtocolService signalProtocolService;

    public SignalProtocolController(DeviceAuthenticator deviceAuthenticator, SignalProtocolService signalProtocolService) {
        this.deviceAuthenticator = deviceAuthenticator;
        this.signalProtocolService = signalProtocolService;
    }

    @PostMapping("/device-bundle")
    @ResponseStatus(HttpStatus.CREATED)
    public SignalDeviceDescriptorResponse registerBundle(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @Valid @RequestBody RegisterSignalDeviceBundleRequest request
    ) {
        return signalProtocolService.registerBundle(deviceAuthenticator.require(deviceToken), request);
    }

    @GetMapping("/accounts/{username}/devices")
    public List<SignalDeviceDescriptorResponse> listPeerDevices(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @PathVariable String username
    ) {
        return signalProtocolService.listPeerDevices(deviceAuthenticator.require(deviceToken), username);
    }

    @PostMapping("/devices/{deviceId}/prekey-bundle:claim")
    public SignalPrekeyBundleResponse claimPeerBundle(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @PathVariable UUID deviceId
    ) {
        return signalProtocolService.claimPeerBundle(deviceAuthenticator.require(deviceToken), deviceId);
    }

    @PostMapping("/devices/{deviceId}/identity-verification")
    public SignalIdentityVerificationResponse verifyIdentity(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @PathVariable UUID deviceId,
            @Valid @RequestBody VerifySignalIdentityRequest request
    ) {
        return signalProtocolService.verifyIdentity(deviceAuthenticator.require(deviceToken), deviceId, request);
    }

    @GetMapping("/devices/{deviceId}/identity-verification")
    public SignalIdentityVerificationResponse getIdentityVerification(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @PathVariable UUID deviceId
    ) {
        return signalProtocolService.getIdentityVerification(deviceAuthenticator.require(deviceToken), deviceId);
    }
}
