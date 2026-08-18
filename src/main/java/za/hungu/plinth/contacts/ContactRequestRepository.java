package za.hungu.plinth.contacts;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ContactRequestRepository extends JpaRepository<ContactRequest, UUID> {

    Optional<ContactRequest> findBySenderAccountIdAndRecipientAccountId(UUID senderAccountId, UUID recipientAccountId);

    Optional<ContactRequest> findByIdAndRecipientAccountId(UUID id, UUID recipientAccountId);

    boolean existsBySenderAccountIdAndRecipientAccountIdAndStatus(
            UUID senderAccountId,
            UUID recipientAccountId,
            ContactRequestStatus status
    );
}
