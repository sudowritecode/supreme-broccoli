package za.hungu.plinth.miniapps;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record MiniAppLaunchTicketResponse(
        UUID ticketId,
        String appId,
        String appVersion,
        String origin,
        UUID accountId,
        UUID deviceId,
        Set<MiniAppPermission> permissions,
        String nonce,
        Instant expiresAt,
        String ticketSignatureBase64,
        String platformPublicKeyBase64,
        boolean consumed
) {
}
