package za.hungu.plinth.signal;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "plinth.signal-protocol")
public record SignalProtocolProperties(boolean enabled) {
}
