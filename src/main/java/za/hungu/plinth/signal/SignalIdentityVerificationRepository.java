package za.hungu.plinth.signal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SignalIdentityVerificationRepository extends JpaRepository<SignalIdentityVerification, UUID> {
    Optional<SignalIdentityVerification> findByVerifierDeviceIdAndSubjectDeviceId(UUID verifierDeviceId, UUID subjectDeviceId);
    List<SignalIdentityVerification> findAllBySubjectDeviceId(UUID subjectDeviceId);
}
