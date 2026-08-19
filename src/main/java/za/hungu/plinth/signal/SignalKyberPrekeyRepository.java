package za.hungu.plinth.signal;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface SignalKyberPrekeyRepository extends JpaRepository<SignalKyberPrekey, UUID> {
    boolean existsByDeviceIdAndPrekeyId(UUID deviceId, long prekeyId);

    Optional<SignalKyberPrekey> findFirstByDeviceIdAndLastResortTrueOrderByCreatedAtDesc(UUID deviceId);

    long countByDeviceIdAndLastResortFalseAndClaimedAtIsNull(UUID deviceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SignalKyberPrekey> findFirstByDeviceIdAndLastResortFalseAndClaimedAtIsNullOrderByCreatedAtAsc(UUID deviceId);
}
