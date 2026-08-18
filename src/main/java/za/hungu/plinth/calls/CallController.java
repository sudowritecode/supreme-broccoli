package za.hungu.plinth.calls;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import za.hungu.plinth.auth.DeviceAuthenticator;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/calls")
public class CallController {

    private final DeviceAuthenticator deviceAuthenticator;
    private final CallService callService;

    public CallController(DeviceAuthenticator deviceAuthenticator, CallService callService) {
        this.deviceAuthenticator = deviceAuthenticator;
        this.callService = callService;
    }

    @PostMapping("/conversations/{conversationId}")
    @ResponseStatus(HttpStatus.CREATED)
    public CallResponse start(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @PathVariable UUID conversationId
    ) {
        return callService.start(deviceAuthenticator.require(deviceToken), conversationId);
    }

    @PostMapping("/{callSessionId}/join")
    public CallParticipantResponse join(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @PathVariable UUID callSessionId
    ) {
        return callService.join(deviceAuthenticator.require(deviceToken), callSessionId);
    }

    @PostMapping("/{callSessionId}/leave")
    public CallParticipantResponse leave(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @PathVariable UUID callSessionId
    ) {
        return callService.leave(deviceAuthenticator.require(deviceToken), callSessionId);
    }

    @PostMapping("/{callSessionId}/end")
    public CallResponse end(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @PathVariable UUID callSessionId
    ) {
        return callService.end(deviceAuthenticator.require(deviceToken), callSessionId);
    }
}
