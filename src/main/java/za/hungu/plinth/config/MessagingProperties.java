package za.hungu.plinth.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "plinth.messaging")
public record MessagingProperties(
        boolean enabled,
        @NotBlank String exchange,
        @NotBlank String outboundQueue,
        @NotBlank String deadLetterQueue,
        @NotBlank String routingKey
) {
}
