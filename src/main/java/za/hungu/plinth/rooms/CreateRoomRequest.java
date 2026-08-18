package za.hungu.plinth.rooms;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateRoomRequest(
        @NotBlank @Size(max = 120) String topic,
        @Min(2) @Max(50) int capacity,
        @NotEmpty @Size(max = 8) Set<@NotBlank @Size(max = 32) String> interestTags
) {
}
