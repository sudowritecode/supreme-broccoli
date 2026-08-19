package za.hungu.plinth.miniapps;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "plinth.mini-apps")
public record MiniAppProperties(
        boolean enabled,
        String registrationKey,
        String ticketPrivateKeyBase64,
        String ticketPublicKeyBase64,
        long ticketLifetimeSeconds
) {
}
