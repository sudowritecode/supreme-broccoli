package za.hungu.plinth.payments;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "plinth.payments")
public record PaymentProperties(boolean enabled, boolean liveEnabled) {
}
