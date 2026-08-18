package za.hungu.plinth.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "plinth.messaging", name = "enabled", havingValue = "true")
public class RabbitMqTopologyConfiguration {

    @Bean
    TopicExchange messagingExchange(MessagingProperties properties) {
        return new TopicExchange(properties.exchange(), true, false);
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange("plinth.dead-letter", true, false);
    }

    @Bean
    Queue outboundMessageQueue(MessagingProperties properties) {
        return QueueBuilder.durable(properties.outboundQueue())
                .deadLetterExchange("plinth.dead-letter")
                .deadLetterRoutingKey(properties.deadLetterQueue())
                .build();
    }

    @Bean
    Queue outboundMessageDeadLetterQueue(MessagingProperties properties) {
        return QueueBuilder.durable(properties.deadLetterQueue()).build();
    }

    @Bean
    Binding outboundMessageBinding(
            Queue outboundMessageQueue,
            TopicExchange messagingExchange,
            MessagingProperties properties
    ) {
        return BindingBuilder.bind(outboundMessageQueue)
                .to(messagingExchange)
                .with(properties.routingKey());
    }

    @Bean
    Binding outboundMessageDeadLetterBinding(
            Queue outboundMessageDeadLetterQueue,
            DirectExchange deadLetterExchange,
            MessagingProperties properties
    ) {
        return BindingBuilder.bind(outboundMessageDeadLetterQueue)
                .to(deadLetterExchange)
                .with(properties.deadLetterQueue());
    }
}
