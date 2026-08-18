package za.hungu.plinth.calls;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CallParticipantRepository extends JpaRepository<CallParticipant, CallParticipant.CallParticipantId> {

    Optional<CallParticipant> findByCallSessionIdAndAccountId(UUID callSessionId, UUID accountId);
}
