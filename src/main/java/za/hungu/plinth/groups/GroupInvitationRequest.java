package za.hungu.plinth.groups;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GroupInvitationRequest(
        @NotBlank @Size(max = 40) String username
) {
}
