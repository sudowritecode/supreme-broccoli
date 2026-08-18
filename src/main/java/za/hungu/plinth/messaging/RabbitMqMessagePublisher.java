package za.hungu.plinth.messaging;

import za.hungu.plinth.config.MessagingProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "plinth.messaging", name = "enabled", havingValue = "true")
public class RabbitMqMessagePublisher implements MessagePublisher {

    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties properties;

    public RabbitMqMessagePublisher(RabbitTemplate rabbitTemplate, MessagingProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @Override
    public void publish(EncryptedMessageEvent event) {
        rabbitTemplate.convertAndSend(properties.exchange(), properties.routingKey(), event);
    }
}
