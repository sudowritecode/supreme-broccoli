package za.hungu.plinth.identity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<Device, UUID> {

    Optional<Device> findByAccessTokenHashAndRevokedAtIsNull(String accessTokenHash);

    Optional<Device> findByIdAndAccountIdAndRevokedAtIsNull(UUID id, UUID accountId);

    Optional<Device> findByIdAndRevokedAtIsNull(UUID id);
}
