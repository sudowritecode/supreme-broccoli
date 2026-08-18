package za.hungu.plinth.rooms;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import za.hungu.plinth.auth.DeviceAuthenticator;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {
    private final DeviceAuthenticator deviceAuthenticator;
    private final RoomService roomService;

    public RoomController(DeviceAuthenticator deviceAuthenticator, RoomService roomService) {
        this.deviceAuthenticator = deviceAuthenticator;
        this.roomService = roomService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse create(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @Valid @RequestBody CreateRoomRequest request
    ) {
        return roomService.create(deviceAuthenticator.require(deviceToken), request);
    }

    @PutMapping("/interest-preferences")
    public List<String> updateInterestPreferences(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @Valid @RequestBody UpdateInterestPreferencesRequest request
    ) {
        return roomService.updateInterestPreferences(deviceAuthenticator.require(deviceToken), request);
    }

    @GetMapping("/suggestions")
    public List<RoomSuggestionResponse> suggestions(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken
    ) {
        return roomService.suggestions(deviceAuthenticator.require(deviceToken));
    }

    @PostMapping("/{roomId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomParticipantResponse invite(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @PathVariable UUID roomId,
            @Valid @RequestBody RoomInvitationRequest request
    ) {
        return roomService.invite(deviceAuthenticator.require(deviceToken), roomId, request);
    }

    @PostMapping("/{roomId}/entry-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomParticipantResponse requestEntry(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @PathVariable UUID roomId
    ) {
        return roomService.requestEntry(deviceAuthenticator.require(deviceToken), roomId);
    }

    @PostMapping("/{roomId}/participants/{accountId}/co-host")
    public RoomParticipantResponse promoteToCoHost(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @PathVariable UUID roomId,
            @PathVariable UUID accountId
    ) {
        return roomService.promoteToCoHost(deviceAuthenticator.require(deviceToken), roomId, accountId);
    }

    @PostMapping("/{roomId}/participants/{accountId}/admit")
    public RoomParticipantResponse admit(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @PathVariable UUID roomId,
            @PathVariable UUID accountId
    ) {
        return roomService.admit(deviceAuthenticator.require(deviceToken), roomId, accountId);
    }

    @PostMapping("/{roomId}/leave")
    public RoomParticipantResponse leave(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @PathVariable UUID roomId
    ) {
        return roomService.leave(deviceAuthenticator.require(deviceToken), roomId);
    }

    @DeleteMapping("/{roomId}/participants/{accountId}")
    public RoomParticipantResponse remove(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @PathVariable UUID roomId,
            @PathVariable UUID accountId
    ) {
        return roomService.remove(deviceAuthenticator.require(deviceToken), roomId, accountId);
    }

    @PostMapping("/{roomId}/end")
    public RoomResponse end(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @PathVariable UUID roomId
    ) {
        return roomService.end(deviceAuthenticator.require(deviceToken), roomId);
    }

    @PostMapping("/{roomId}/blocks")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void block(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @PathVariable UUID roomId,
            @Valid @RequestBody RoomBlockRequest request
    ) {
        roomService.block(deviceAuthenticator.require(deviceToken), roomId, request);
    }

    @PostMapping("/{roomId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public UUID report(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @PathVariable UUID roomId,
            @Valid @RequestBody CreateRoomReportRequest request
    ) {
        return roomService.report(deviceAuthenticator.require(deviceToken), roomId, request);
    }
}
