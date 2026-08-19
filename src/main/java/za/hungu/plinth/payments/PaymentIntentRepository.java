package za.hungu.plinth.payments;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentIntentRepository extends JpaRepository<PaymentIntent, UUID> {
    Optional<PaymentIntent> findByOwnerAccountIdAndIdempotencyKey(UUID ownerAccountId, String idempotencyKey);
    Optional<PaymentIntent> findByIdAndOwnerAccountId(UUID id, UUID ownerAccountId);
}
