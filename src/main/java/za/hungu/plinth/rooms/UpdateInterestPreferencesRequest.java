package za.hungu.plinth.rooms;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UpdateInterestPreferencesRequest(
        @NotEmpty @Size(max = 16) Set<@NotBlank @Size(max = 32) String> interestTags
) {
}
