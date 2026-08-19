package za.hungu.plinth.signal;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SignalOneTimePrekeyRepository extends JpaRepository<SignalOneTimePrekey, UUID> {
    boolean existsByDeviceIdAndPrekeyId(UUID deviceId, long prekeyId);

    long countByDeviceIdAndClaimedAtIsNull(UUID deviceId);

    List<SignalOneTimePrekey> findAllByDeviceIdAndClaimedAtIsNullOrderByCreatedAtAsc(UUID deviceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SignalOneTimePrekey> findFirstByDeviceIdAndClaimedAtIsNullOrderByCreatedAtAsc(UUID deviceId);
}
