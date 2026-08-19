package za.hungu.plinth.signal;

import java.time.Instant;
import java.util.UUID;

public record SignalDeviceDescriptorResponse(
        UUID deviceId,
        String protocolAddressName,
        String label,
        SignalProtocolProfile protocolProfile,
        int protocolDeviceId,
        int registrationId,
        Instant signedPrekeyExpiresAt,
        long availableOneTimePrekeys
) {
}
