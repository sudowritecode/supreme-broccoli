package za.hungu.plinth.outbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnProperty(prefix = "plinth.messaging", name = "enabled", havingValue = "true")
public class MessageOutboxRelay {

    private final MessageOutboxRepository messageOutboxRepository;
    private final MessageOutboxPublisher messageOutboxPublisher;

    public MessageOutboxRelay(
            MessageOutboxRepository messageOutboxRepository,
            MessageOutboxPublisher messageOutboxPublisher
    ) {
        this.messageOutboxRepository = messageOutboxRepository;
        this.messageOutboxPublisher = messageOutboxPublisher;
    }

    @TransactionalEventListener
    public void relayNewMessage(EncryptedMessageQueued event) {
        messageOutboxPublisher.publishPending(event.outboxId());
    }

    @Scheduled(fixedDelayString = "${plinth.messaging.relay-fixed-delay-ms:5000}")
    public void retryPendingMessages() {
        messageOutboxRepository.findTop100ByStatusOrderByReceivedAtAsc(OutboxStatus.PENDING)
                .forEach(outbox -> messageOutboxPublisher.publishPending(outbox.getId()));
    }
}
