package za.hungu.plinth.calls;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CallSessionRepository extends JpaRepository<CallSession, UUID> {

    Optional<CallSession> findFirstByConversationIdAndStatusOrderByStartedAtDesc(UUID conversationId, CallSessionStatus status);

    List<CallSession> findByConversationIdAndStatus(UUID conversationId, CallSessionStatus status);
}
