package za.hungu.plinth.identity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterAccountRequest(
        @NotBlank
        @Pattern(regexp = "[a-zA-Z0-9_]{3,32}", message = "must contain 3-32 letters, numbers, or underscores")
        String username,
        @NotBlank @Size(max = 120) String deviceLabel,
        @NotBlank @Size(max = 4096) String publicIdentityKey
) {
}
