package za.hungu.plinth.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import za.hungu.plinth.auth.DeviceAuthenticator;
import za.hungu.plinth.messaging.MessageIngressResult;
import za.hungu.plinth.messaging.MessageIngressService;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

    private final DeviceAuthenticator deviceAuthenticator;
    private final MessageIngressService messageIngressService;

    public MessageController(DeviceAuthenticator deviceAuthenticator, MessageIngressService messageIngressService) {
        this.deviceAuthenticator = deviceAuthenticator;
        this.messageIngressService = messageIngressService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MessageAcceptedResponse enqueue(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @Valid @RequestBody SendEncryptedMessageRequest request
    ) {
        MessageIngressResult result = messageIngressService.queue(deviceAuthenticator.require(deviceToken), request);
        return new MessageAcceptedResponse(result.messageId(), result.receivedAt(), result.duplicate() ? "duplicate" : "queued");
    }

    public record MessageAcceptedResponse(UUID messageId, Instant receivedAt, String status) {
    }
}
