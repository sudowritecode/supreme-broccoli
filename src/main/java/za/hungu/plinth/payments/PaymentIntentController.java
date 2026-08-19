package za.hungu.plinth.payments;

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
@RequestMapping("/api/v1/payments")
public class PaymentIntentController {
    public static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";

    private final DeviceAuthenticator deviceAuthenticator;
    private final PaymentIntentService paymentIntentService;

    public PaymentIntentController(DeviceAuthenticator deviceAuthenticator, PaymentIntentService paymentIntentService) {
        this.deviceAuthenticator = deviceAuthenticator;
        this.paymentIntentService = paymentIntentService;
    }

    @PostMapping("/intents")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentIntentResponse create(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @RequestHeader(name = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody CreatePaymentIntentRequest request
    ) {
        return paymentIntentService.create(deviceAuthenticator.require(deviceToken), idempotencyKey, request);
    }

    @GetMapping("/intents/{paymentIntentId}")
    public PaymentIntentResponse get(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @PathVariable UUID paymentIntentId
    ) {
        return paymentIntentService.get(deviceAuthenticator.require(deviceToken), paymentIntentId);
    }

    @PostMapping("/intents/{paymentIntentId}/authorize")
    public PaymentIntentResponse authorize(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @PathVariable UUID paymentIntentId
    ) {
        return paymentIntentService.authorize(deviceAuthenticator.require(deviceToken), paymentIntentId);
    }

    @PostMapping("/intents/{paymentIntentId}/cancel")
    public PaymentIntentResponse cancel(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @PathVariable UUID paymentIntentId
    ) {
        return paymentIntentService.cancel(deviceAuthenticator.require(deviceToken), paymentIntentId);
    }

    @PostMapping("/live/activation")
    @ResponseStatus(HttpStatus.CONFLICT)
    public void refuseLiveActivation(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken
    ) {
        paymentIntentService.refuseLiveActivation(deviceAuthenticator.require(deviceToken));
    }
}
