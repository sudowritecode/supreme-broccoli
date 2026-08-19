package za.hungu.plinth.signal;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class SignalProtocolFeatureGuard {
    private final SignalProtocolProperties properties;

    public SignalProtocolFeatureGuard(SignalProtocolProperties properties) {
        this.properties = properties;
    }

    public void requireEnabled() {
        if (!properties.enabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Signal protocol integration is not enabled.");
        }
    }
}
