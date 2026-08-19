package za.hungu.plinth.miniapps;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class MiniAppFeatureGuard {
    private final MiniAppProperties properties;

    public MiniAppFeatureGuard(MiniAppProperties properties) {
        this.properties = properties;
    }

    public void requireEnabled() {
        if (!properties.enabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mini apps are not enabled.");
        }
    }

    public void requireRegistrationKey(String providedKey) {
        requireEnabled();
        if (properties.registrationKey() == null || properties.registrationKey().isBlank() ||
                !java.security.MessageDigest.isEqual(
                        properties.registrationKey().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        (providedKey == null ? "" : providedKey).getBytes(java.nio.charset.StandardCharsets.UTF_8)
                )) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A valid mini-app registration key is required.");
        }
    }

    public MiniAppProperties configuredProperties() {
        requireEnabled();
        if (properties.ticketPrivateKeyBase64() == null || properties.ticketPrivateKeyBase64().isBlank() ||
                properties.ticketPublicKeyBase64() == null || properties.ticketPublicKeyBase64().isBlank() ||
                properties.ticketLifetimeSeconds() <= 0 || properties.ticketLifetimeSeconds() > 900) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Mini-app ticket signing is not configured.");
        }
        return properties;
    }
}
