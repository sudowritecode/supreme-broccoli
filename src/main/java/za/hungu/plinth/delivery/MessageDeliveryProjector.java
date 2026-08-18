package za.hungu.plinth.delivery;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import za.hungu.plinth.outbox.EncryptedMessageQueued;

@Component
public class MessageDeliveryProjector {

    private final MessageDeliveryService messageDeliveryService;

    public MessageDeliveryProjector(MessageDeliveryService messageDeliveryService) {
        this.messageDeliveryService = messageDeliveryService;
    }

    @TransactionalEventListener
    public void createRecipientDelivery(EncryptedMessageQueued event) {
        MessageDelivery delivery = messageDeliveryService.ensurePendingDelivery(event.outboxId());
        messageDeliveryService.attemptDelivery(delivery.getId());
    }
}
