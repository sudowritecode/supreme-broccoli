package za.hungu.plinth.signal;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class SignalOpaqueEnvelopeCodec {
    private static final String SEPARATOR = ".";

    public String encode(SignalProtocolProfile profile, SignalEnvelopeKind kind, String serializedCiphertextBase64) {
        return profile.name() + SEPARATOR + kind.name() + SEPARATOR + serializedCiphertextBase64;
    }

    public SignalOpaqueEnvelope decode(String ciphertext) {
        String[] fields = ciphertext.split("\\.", 3);
        if (fields.length != 3 || fields[2].isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A Signal envelope must include profile, kind, and opaque serialized ciphertext.");
        }
        try {
            return new SignalOpaqueEnvelope(
                    SignalProtocolProfile.valueOf(fields[0]),
                    SignalEnvelopeKind.valueOf(fields[1]),
                    fields[2]
            );
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported Signal protocol profile or envelope kind.");
        }
    }

    public record SignalOpaqueEnvelope(
            SignalProtocolProfile profile,
            SignalEnvelopeKind kind,
            String serializedCiphertextBase64
    ) {
    }
}
