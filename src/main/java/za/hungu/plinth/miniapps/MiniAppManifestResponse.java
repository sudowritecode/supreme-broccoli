package za.hungu.plinth.miniapps;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record MiniAppManifestResponse(
        UUID manifestId,
        String appId,
        String appVersion,
        String issuer,
        String origin,
        String publicKeyBase64,
        String signatureBase64,
        Set<MiniAppPermission> permissions,
        Instant createdAt
) {
}
