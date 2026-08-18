package za.hungu.plinth.groups;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import za.hungu.plinth.auth.DeviceAuthenticator;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {

    private final DeviceAuthenticator deviceAuthenticator;
    private final GroupService groupService;

    public GroupController(DeviceAuthenticator deviceAuthenticator, GroupService groupService) {
        this.deviceAuthenticator = deviceAuthenticator;
        this.groupService = groupService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse create(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @Valid @RequestBody CreateGroupRequest request
    ) {
        return groupService.create(deviceAuthenticator.require(deviceToken), request);
    }

    @PostMapping("/{groupId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupMemberResponse invite(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @PathVariable UUID groupId,
            @Valid @RequestBody GroupInvitationRequest request
    ) {
        return groupService.invite(deviceAuthenticator.require(deviceToken), groupId, request);
    }

    @PostMapping("/{groupId}/invitations/accept")
    public GroupMemberResponse accept(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @PathVariable UUID groupId
    ) {
        return groupService.accept(deviceAuthenticator.require(deviceToken), groupId);
    }

    @PostMapping("/{groupId}/invitations/decline")
    public GroupMemberResponse decline(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @PathVariable UUID groupId
    ) {
        return groupService.decline(deviceAuthenticator.require(deviceToken), groupId);
    }

    @PostMapping("/{groupId}/leave")
    public GroupMemberResponse leave(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @PathVariable UUID groupId
    ) {
        return groupService.leave(deviceAuthenticator.require(deviceToken), groupId);
    }

    @DeleteMapping("/{groupId}/members/{accountId}")
    public GroupMemberResponse remove(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @PathVariable UUID groupId,
            @PathVariable UUID accountId
    ) {
        return groupService.remove(deviceAuthenticator.require(deviceToken), groupId, accountId);
    }
}
