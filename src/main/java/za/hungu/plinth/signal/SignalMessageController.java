package za.hungu.plinth.signal;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import za.hungu.plinth.api.SendEncryptedMessageRequest;
import za.hungu.plinth.auth.DeviceAuthenticator;
import za.hungu.plinth.messaging.MessageIngressResult;
import za.hungu.plinth.messaging.MessageIngressService;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/signal/messages")
public class SignalMessageController {
    private final DeviceAuthenticator deviceAuthenticator;
    private final SignalProtocolFeatureGuard featureGuard;
    private final SignalOpaqueEnvelopeCodec envelopeCodec;
    private final MessageIngressService messageIngressService;

    public SignalMessageController(
            DeviceAuthenticator deviceAuthenticator,
            SignalProtocolFeatureGuard featureGuard,
            SignalOpaqueEnvelopeCodec envelopeCodec,
            MessageIngressService messageIngressService
    ) {
        this.deviceAuthenticator = deviceAuthenticator;
        this.featureGuard = featureGuard;
        this.envelopeCodec = envelopeCodec;
        this.messageIngressService = messageIngressService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SignalMessageAcceptedResponse enqueue(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @Valid @RequestBody SendEncryptedMessageRequest request
    ) {
        featureGuard.requireEnabled();
        SignalOpaqueEnvelopeCodec.SignalOpaqueEnvelope envelope = envelopeCodec.decode(request.ciphertext());
        if (envelope.kind() == SignalEnvelopeKind.GROUP) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Group Signal envelopes require the separately versioned group encryption integration."
            );
        }
        MessageIngressResult result = messageIngressService.queue(deviceAuthenticator.require(deviceToken), request);
        return new SignalMessageAcceptedResponse(
                result.messageId(), result.receivedAt(), result.duplicate() ? "duplicate" : "queued",
                envelope.profile(), envelope.kind()
        );
    }

    public record SignalMessageAcceptedResponse(
            UUID messageId,
            Instant receivedAt,
            String status,
            SignalProtocolProfile protocolProfile,
            SignalEnvelopeKind envelopeKind
    ) {
    }
}
