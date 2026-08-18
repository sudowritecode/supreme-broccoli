package za.hungu.plinth.delivery;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageDeliveryRepository extends JpaRepository<MessageDelivery, UUID> {

    Optional<MessageDelivery> findByOutboxIdAndRecipientDeviceId(UUID outboxId, UUID recipientDeviceId);

    List<MessageDelivery> findByRecipientDeviceIdAndStatusOrderByCreatedAtAsc(
            UUID recipientDeviceId,
            MessageDeliveryStatus status
    );

    List<MessageDelivery> findByOutboxIdInAndRecipientDeviceId(Collection<UUID> outboxIds, UUID recipientDeviceId);
}
