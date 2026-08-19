package za.hungu.plinth.signal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record RegisterSignalDeviceBundleRequest(
        @NotNull SignalProtocolProfile protocolProfile,
        @Min(1) @Max(Integer.MAX_VALUE) int protocolDeviceId,
        @Min(1) @Max(Integer.MAX_VALUE) int registrationId,
        @NotBlank @Size(max = 4096) String identityKey,
        @Min(1) long signedPrekeyId,
        @NotBlank @Size(max = 4096) String signedPrekeyPublic,
        @NotBlank @Size(max = 4096) String signedPrekeySignature,
        @NotNull @Future Instant signedPrekeyExpiresAt,
        @NotNull @Valid SignalKyberPrekeyRequest kyberLastResortPrekey,
        @NotEmpty List<@Valid SignalKyberPrekeyRequest> kyberOneTimePrekeys,
        @NotEmpty List<@Valid SignalOneTimePrekeyRequest> oneTimePrekeys
) {
}
