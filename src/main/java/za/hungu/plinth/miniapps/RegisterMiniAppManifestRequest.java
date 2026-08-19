package za.hungu.plinth.miniapps;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record RegisterMiniAppManifestRequest(
        @NotBlank @Pattern(regexp = "[a-z][a-z0-9-]{2,63}") String appId,
        @NotBlank @Pattern(regexp = "[0-9]+\\.[0-9]+\\.[0-9]+") String appVersion,
        @NotBlank @Size(max = 120) String issuer,
        @NotBlank @Pattern(regexp = "https://[^\\s/]+(?:/[^\\s]*)?") @Size(max = 255) String origin,
        @NotBlank @Size(max = 1024) String publicKeyBase64,
        @NotBlank @Size(max = 1024) String signatureBase64,
        @NotEmpty @Size(max = 2) Set<MiniAppPermission> permissions
) {
}
