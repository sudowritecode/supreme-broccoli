package za.hungu.plinth.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.hungu.plinth.messaging.EncryptedMessageEvent;
import za.hungu.plinth.messaging.MessagePublisher;

import java.time.Instant;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "plinth.messaging", name = "enabled", havingValue = "true")
public class MessageOutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(MessageOutboxPublisher.class);

    private final MessageOutboxRepository messageOutboxRepository;
    private final MessagePublisher messagePublisher;

    public MessageOutboxPublisher(MessageOutboxRepository messageOutboxRepository, MessagePublisher messagePublisher) {
        this.messageOutboxRepository = messageOutboxRepository;
        this.messagePublisher = messagePublisher;
    }

    @Transactional
    public void publishPending(UUID outboxId) {
        MessageOutbox outbox = messageOutboxRepository.findById(outboxId).orElse(null);
        if (outbox == null || outbox.getStatus() != OutboxStatus.PENDING) {
            return;
        }

        try {
            messagePublisher.publish(new EncryptedMessageEvent(
                    outbox.getMessageId(),
                    outbox.getConversationId(),
                    outbox.getSenderDeviceId(),
                    outbox.getRecipientDeviceId(),
                    outbox.getCiphertext(),
                    outbox.getReceivedAt()
            ));
            outbox.markPublished(Instant.now());
        } catch (RuntimeException exception) {
            outbox.recordFailure(exception.getClass().getSimpleName());
            log.warn("Encrypted message publication failed for outboxId={}, messageId={}", outbox.getId(), outbox.getMessageId());
        }
    }
}
