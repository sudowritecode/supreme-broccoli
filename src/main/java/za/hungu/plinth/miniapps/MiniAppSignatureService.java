package za.hungu.plinth.miniapps;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Comparator;
import java.util.Set;
import java.util.UUID;

@Service
public class MiniAppSignatureService {
    public boolean verifyManifest(
            String appId,
            String appVersion,
            String issuer,
            String origin,
            Set<MiniAppPermission> permissions,
            String publicKeyBase64,
            String signatureBase64
    ) {
        try {
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(publicKey(publicKeyBase64));
            verifier.update(manifestPayload(appId, appVersion, issuer, origin, permissions));
            return verifier.verify(Base64.getDecoder().decode(signatureBase64));
        } catch (Exception exception) {
            return false;
        }
    }

    public String signTicket(
            String ticketPrivateKeyBase64,
            UUID ticketId,
            String appId,
            String appVersion,
            UUID accountId,
            UUID deviceId,
            String nonce,
            long expiresAtEpochSecond,
            Set<MiniAppPermission> permissions
    ) {
        try {
            Signature signer = Signature.getInstance("Ed25519");
            signer.initSign(privateKey(ticketPrivateKeyBase64));
            signer.update(ticketPayload(ticketId, appId, appVersion, accountId, deviceId, nonce, expiresAtEpochSecond, permissions));
            return Base64.getEncoder().encodeToString(signer.sign());
        } catch (Exception exception) {
            throw new IllegalStateException("Mini-app ticket signing is not configured correctly.", exception);
        }
    }

    public byte[] manifestPayload(String appId, String appVersion, String issuer, String origin, Set<MiniAppPermission> permissions) {
        return (
                "appId=" + appId + "\n" +
                "appVersion=" + appVersion + "\n" +
                "issuer=" + issuer + "\n" +
                "origin=" + origin + "\n" +
                "permissions=" + canonicalPermissions(permissions) + "\n"
        ).getBytes(StandardCharsets.UTF_8);
    }

    public byte[] ticketPayload(
            UUID ticketId,
            String appId,
            String appVersion,
            UUID accountId,
            UUID deviceId,
            String nonce,
            long expiresAtEpochSecond,
            Set<MiniAppPermission> permissions
    ) {
        return (
                "ticketId=" + ticketId + "\n" +
                "appId=" + appId + "\n" +
                "appVersion=" + appVersion + "\n" +
                "accountId=" + accountId + "\n" +
                "deviceId=" + deviceId + "\n" +
                "nonce=" + nonce + "\n" +
                "expiresAt=" + expiresAtEpochSecond + "\n" +
                "permissions=" + canonicalPermissions(permissions) + "\n"
        ).getBytes(StandardCharsets.UTF_8);
    }

    private String canonicalPermissions(Set<MiniAppPermission> permissions) {
        return permissions.stream().map(Enum::name).sorted(Comparator.naturalOrder()).reduce((left, right) -> left + "," + right).orElse("");
    }

    private PublicKey publicKey(String encoded) throws Exception {
        return KeyFactory.getInstance("Ed25519").generatePublic(
                new X509EncodedKeySpec(Base64.getDecoder().decode(encoded))
        );
    }

    private PrivateKey privateKey(String encoded) throws Exception {
        return KeyFactory.getInstance("Ed25519").generatePrivate(
                new PKCS8EncodedKeySpec(Base64.getDecoder().decode(encoded))
        );
    }
}
