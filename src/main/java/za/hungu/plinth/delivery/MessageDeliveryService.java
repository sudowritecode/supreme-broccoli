package za.hungu.plinth.delivery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import za.hungu.plinth.outbox.MessageOutbox;
import za.hungu.plinth.outbox.MessageOutboxRepository;
import za.hungu.plinth.realtime.DeliveryWebSocketRegistry;

import java.time.Instant;
import java.util.UUID;

@Service
public class MessageDeliveryService {

    private final MessageDeliveryRepository messageDeliveryRepository;
    private final MessageOutboxRepository messageOutboxRepository;
    private final DeliveryWebSocketRegistry webSocketRegistry;
    private final ObjectMapper objectMapper;

    public MessageDeliveryService(
            MessageDeliveryRepository messageDeliveryRepository,
            MessageOutboxRepository messageOutboxRepository,
            DeliveryWebSocketRegistry webSocketRegistry,
            ObjectMapper objectMapper
    ) {
        this.messageDeliveryRepository = messageDeliveryRepository;
        this.messageOutboxRepository = messageOutboxRepository;
        this.webSocketRegistry = webSocketRegistry;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MessageDelivery ensurePendingDelivery(UUID outboxId) {
        MessageOutbox outbox = messageOutboxRepository.findById(outboxId)
                .orElseThrow(() -> new IllegalStateException("Encrypted message outbox record does not exist."));
        return messageDeliveryRepository.findByOutboxIdAndRecipientDeviceId(outboxId, outbox.getRecipientDeviceId())
                .orElseGet(() -> messageDeliveryRepository.save(MessageDelivery.pending(
                        outbox.getId(),
                        outbox.getRecipientDeviceId(),
                        Instant.now()
                )));
    }

    public void attemptDelivery(UUID deliveryId) {
        MessageDelivery delivery = messageDeliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown encrypted-message delivery."));
        MessageOutbox outbox = messageOutboxRepository.findById(delivery.getOutboxId())
                .orElseThrow(() -> new IllegalStateException("Encrypted message outbox record does not exist."));
        try {
            boolean deliveredToLiveSession = webSocketRegistry.send(delivery.getRecipientDeviceId(), objectMapper.writeValueAsString(
                    new EncryptedMessageDeliveryEnvelope(
                            EncryptedMessageDeliveryEnvelope.TYPE,
                            delivery.getId(),
                            outbox.getMessageId(),
                            outbox.getConversationId(),
                            outbox.getSenderDeviceId(),
                            outbox.getCiphertext(),
                            outbox.getReceivedAt()
                    )
            ));
            if (deliveredToLiveSession) {
                recordAttempt(deliveryId);
            }
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not encode encrypted delivery envelope.", exception);
        }
    }

    public void replayPendingForDevice(UUID recipientDeviceId) {
        messageDeliveryRepository.findByRecipientDeviceIdAndStatusOrderByCreatedAtAsc(
                        recipientDeviceId,
                        MessageDeliveryStatus.PENDING
                )
                .forEach(delivery -> attemptDelivery(delivery.getId()));
    }

    @Transactional
    public boolean acknowledge(UUID recipientDeviceId, UUID deliveryId, UUID messageId) {
        MessageDelivery delivery = messageDeliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown delivery acknowledgement."));
        if (!delivery.getRecipientDeviceId().equals(recipientDeviceId)) {
            throw new IllegalArgumentException("Delivery acknowledgement is not authorised for this device.");
        }
        MessageOutbox outbox = messageOutboxRepository.findById(delivery.getOutboxId())
                .orElseThrow(() -> new IllegalStateException("Encrypted message outbox record does not exist."));
        if (!outbox.getMessageId().equals(messageId)) {
            throw new IllegalArgumentException("Delivery acknowledgement does not match the encrypted message.");
        }
        return delivery.markDelivered(Instant.now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAttempt(UUID deliveryId) {
        MessageDelivery delivery = messageDeliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown encrypted-message delivery."));
        delivery.recordAttempt(Instant.now());
        messageDeliveryRepository.save(delivery);
    }
}
