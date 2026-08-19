package za.hungu.plinth.miniapps;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MiniAppLaunchTicketRepository extends JpaRepository<MiniAppLaunchTicket, UUID> {
    Optional<MiniAppLaunchTicket> findByIdAndAccountIdAndDeviceId(UUID id, UUID accountId, UUID deviceId);
}
