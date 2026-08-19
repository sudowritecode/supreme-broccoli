package za.hungu.plinth.signal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignalKyberPrekeyRequest(
        @Min(1) long prekeyId,
        @NotBlank @Size(max = 16384) String publicKey,
        @NotBlank @Size(max = 4096) String signature
) {
}
