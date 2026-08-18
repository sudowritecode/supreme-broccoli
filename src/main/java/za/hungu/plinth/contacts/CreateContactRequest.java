package za.hungu.plinth.contacts;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateContactRequest(
        @NotBlank
        @Pattern(regexp = "[a-zA-Z0-9_]{3,32}", message = "must contain 3-32 letters, numbers, or underscores")
        String recipientUsername
) {
}
