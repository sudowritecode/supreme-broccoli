package za.hungu.plinth.miniapps;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MiniAppManifestRepository extends JpaRepository<MiniAppManifest, UUID> {
    Optional<MiniAppManifest> findByAppIdAndAppVersion(String appId, String appVersion);
}
