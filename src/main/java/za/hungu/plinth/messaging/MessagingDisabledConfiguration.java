package za.hungu.plinth.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagingDisabledConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MessagingDisabledConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(MessagePublisher.class)
    MessagePublisher disabledMessagePublisher() {
        return event -> log.debug(
                "Broker publishing disabled for messageId={}, conversationId={}",
                event.messageId(),
                event.conversationId()
        );
    }
}
