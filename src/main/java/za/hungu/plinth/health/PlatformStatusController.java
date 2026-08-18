package za.hungu.plinth.health;

import za.hungu.plinth.config.MessagingProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/platform")
public class PlatformStatusController {

    private final MessagingProperties messagingProperties;

    public PlatformStatusController(MessagingProperties messagingProperties) {
        this.messagingProperties = messagingProperties;
    }

    @GetMapping("/status")
    public PlatformStatusResponse status() {
        return new PlatformStatusResponse("ok", messagingProperties.enabled(), Instant.now());
    }

    public record PlatformStatusResponse(String status, boolean brokerEnabled, Instant checkedAt) {
    }
}
