package za.hungu.plinth.games;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import za.hungu.plinth.auth.DeviceAuthenticator;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/games")
public class GameSessionController {
    private final DeviceAuthenticator deviceAuthenticator;
    private final GameSessionService gameSessionService;

    public GameSessionController(DeviceAuthenticator deviceAuthenticator, GameSessionService gameSessionService) {
        this.deviceAuthenticator = deviceAuthenticator;
        this.gameSessionService = gameSessionService;
    }

    @GetMapping("/catalog")
    public List<CuratedGameDefinition> catalog(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken
    ) {
        deviceAuthenticator.require(deviceToken);
        return gameSessionService.catalog();
    }

    @PostMapping("/rooms/{roomId}/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public GameSessionResponse startForRoom(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @PathVariable UUID roomId,
            @Valid @RequestBody StartGameSessionRequest request
    ) {
        return gameSessionService.startForRoom(deviceAuthenticator.require(deviceToken), roomId, request);
    }

    @PostMapping("/conversations/{conversationId}/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public GameSessionResponse startForConversation(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @PathVariable UUID conversationId,
            @Valid @RequestBody StartGameSessionRequest request
    ) {
        return gameSessionService.startForConversation(deviceAuthenticator.require(deviceToken), conversationId, request);
    }

    @PostMapping("/sessions/{gameSessionId}/join")
    public GameSessionParticipantResponse join(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @PathVariable UUID gameSessionId
    ) {
        return gameSessionService.join(deviceAuthenticator.require(deviceToken), gameSessionId);
    }

    @PostMapping("/sessions/{gameSessionId}/leave")
    public GameSessionParticipantResponse leave(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @PathVariable UUID gameSessionId
    ) {
        return gameSessionService.leave(deviceAuthenticator.require(deviceToken), gameSessionId);
    }

    @PostMapping("/sessions/{gameSessionId}/end")
    public GameSessionResponse end(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @PathVariable UUID gameSessionId
    ) {
        return gameSessionService.end(deviceAuthenticator.require(deviceToken), gameSessionId);
    }
}
