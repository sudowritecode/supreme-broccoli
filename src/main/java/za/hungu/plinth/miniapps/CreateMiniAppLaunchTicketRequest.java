package za.hungu.plinth.miniapps;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateMiniAppLaunchTicketRequest(
        @NotEmpty @Size(max = 2) Set<MiniAppPermission> acceptedPermissions
) {
}
