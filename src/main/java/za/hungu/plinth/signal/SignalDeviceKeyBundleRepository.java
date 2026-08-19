package za.hungu.plinth.signal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SignalDeviceKeyBundleRepository extends JpaRepository<SignalDeviceKeyBundle, UUID> {
    Optional<SignalDeviceKeyBundle> findByDeviceId(UUID deviceId);
}
