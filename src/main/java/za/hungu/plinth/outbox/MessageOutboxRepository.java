package za.hungu.plinth.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageOutboxRepository extends JpaRepository<MessageOutbox, UUID> {

    Optional<MessageOutbox> findByMessageId(UUID messageId);

    List<MessageOutbox> findTop100ByStatusOrderByReceivedAtAsc(OutboxStatus status);
}
