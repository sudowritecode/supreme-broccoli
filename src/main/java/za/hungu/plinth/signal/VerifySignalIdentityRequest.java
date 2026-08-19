package za.hungu.plinth.signal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifySignalIdentityRequest(
        @NotBlank @Size(max = 512) String safetyNumberFingerprint
) {
}
