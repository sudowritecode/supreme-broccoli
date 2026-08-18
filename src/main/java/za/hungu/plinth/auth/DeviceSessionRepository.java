package za.hungu.plinth.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceSessionRepository extends JpaRepository<DeviceSession, UUID> {

    Optional<DeviceSession> findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(String tokenHash, Instant instant);

    List<DeviceSession> findByDeviceIdAndRevokedAtIsNull(UUID deviceId);
}
