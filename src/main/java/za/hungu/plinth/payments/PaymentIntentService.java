package za.hungu.plinth.payments;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import za.hungu.plinth.auth.AuthenticatedDevice;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class PaymentIntentService {
    private final PaymentProperties paymentProperties;
    private final PaymentIntentRepository paymentIntentRepository;
    private final PaymentProviderPort paymentProviderPort;

    public PaymentIntentService(
            PaymentProperties paymentProperties,
            PaymentIntentRepository paymentIntentRepository,
            PaymentProviderPort paymentProviderPort
    ) {
        this.paymentProperties = paymentProperties;
        this.paymentIntentRepository = paymentIntentRepository;
        this.paymentProviderPort = paymentProviderPort;
    }

    @Transactional
    public PaymentIntentResponse create(
            AuthenticatedDevice caller,
            String idempotencyKey,
            CreatePaymentIntentRequest request
    ) {
        requireSandboxEnabled();
        String normalizedKey = requireIdempotencyKey(idempotencyKey);
        String currency = request.currency().toUpperCase(Locale.ROOT);
        var existing = paymentIntentRepository.findByOwnerAccountIdAndIdempotencyKey(caller.accountId(), normalizedKey);
        if (existing.isPresent()) {
            return resolveIdempotent(existing.get(), request.amountMinor(), currency);
        }
        UUID intentId = UUID.randomUUID();
        String providerReference = paymentProviderPort.createSandboxReference(intentId, request.amountMinor(), currency);
        PaymentIntent intent = PaymentIntent.create(
                intentId, caller.accountId(), normalizedKey, request.amountMinor(), currency, providerReference, Instant.now()
        );
        return toResponse(paymentIntentRepository.save(intent));
    }

    @Transactional(readOnly = true)
    public PaymentIntentResponse get(AuthenticatedDevice caller, UUID paymentIntentId) {
        requireSandboxEnabled();
        return toResponse(requireOwnedIntent(caller.accountId(), paymentIntentId));
    }

    @Transactional
    public PaymentIntentResponse authorize(AuthenticatedDevice caller, UUID paymentIntentId) {
        requireSandboxEnabled();
        PaymentIntent intent = requireOwnedIntent(caller.accountId(), paymentIntentId);
        try {
            intent.authorize(Instant.now());
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
        return toResponse(intent);
    }

    @Transactional
    public PaymentIntentResponse cancel(AuthenticatedDevice caller, UUID paymentIntentId) {
        requireSandboxEnabled();
        PaymentIntent intent = requireOwnedIntent(caller.accountId(), paymentIntentId);
        try {
            intent.cancel(Instant.now());
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
        return toResponse(intent);
    }

    public void refuseLiveActivation(AuthenticatedDevice caller) {
        requireSandboxEnabled();
        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Live payment activation is permanently unavailable without a separately shipped regulated partner module."
        );
    }

    private PaymentIntentResponse resolveIdempotent(PaymentIntent existing, long amountMinor, String currency) {
        if (existing.getAmountMinor() != amountMinor || !existing.getCurrency().equals(currency)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An idempotency key cannot be reused with a different amount or currency."
            );
        }
        return toResponse(existing);
    }

    private PaymentIntent requireOwnedIntent(UUID accountId, UUID paymentIntentId) {
        return paymentIntentRepository.findByIdAndOwnerAccountId(paymentIntentId, accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sandbox payment intent was not found."));
    }

    private void requireSandboxEnabled() {
        if (!paymentProperties.enabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sandbox payments are not enabled.");
        }
        if (paymentProperties.liveEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "The base platform refuses live payment execution even when a configuration value is present."
            );
        }
    }

    private String requireIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 128) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A non-blank idempotency key of at most 128 characters is required.");
        }
        return idempotencyKey.trim();
    }

    private PaymentIntentResponse toResponse(PaymentIntent intent) {
        return new PaymentIntentResponse(
                intent.getId(), intent.getAmountMinor(), intent.getCurrency(), intent.getStatus(), intent.getProviderReference(),
                intent.getCreatedAt(), intent.getAuthorizedAt(), intent.getCancelledAt(), true
        );
    }
}
