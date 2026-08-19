package za.hungu.plinth.signal;

import java.time.Instant;
import java.util.UUID;

public record SignalPrekeyBundleResponse(
        UUID deviceId,
        String protocolAddressName,
        SignalProtocolProfile protocolProfile,
        int protocolDeviceId,
        int registrationId,
        String identityKey,
        long signedPrekeyId,
        String signedPrekeyPublic,
        String signedPrekeySignature,
        Instant signedPrekeyExpiresAt,
        SignalOneTimePrekeyResponse oneTimePrekey,
        UUID oneTimePrekeyClaimId,
        SignalKyberPrekeyResponse kyberPrekey,
        UUID kyberPrekeyClaimId,
        long remainingOneTimePrekeys,
        long remainingKyberOneTimePrekeys
) {
}
