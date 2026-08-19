package za.hungu.plinth.signal;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import za.hungu.plinth.auth.AuthenticatedDevice;
import za.hungu.plinth.contacts.ContactRequestRepository;
import za.hungu.plinth.contacts.ContactRequestStatus;
import za.hungu.plinth.identity.Account;
import za.hungu.plinth.identity.AccountRepository;
import za.hungu.plinth.identity.Device;
import za.hungu.plinth.identity.DeviceRepository;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class SignalProtocolService {
    private final SignalProtocolFeatureGuard featureGuard;
    private final DeviceRepository deviceRepository;
    private final AccountRepository accountRepository;
    private final ContactRequestRepository contactRequestRepository;
    private final SignalDeviceKeyBundleRepository keyBundleRepository;
    private final SignalOneTimePrekeyRepository oneTimePrekeyRepository;
    private final SignalKyberPrekeyRepository kyberPrekeyRepository;
    private final SignalIdentityVerificationRepository verificationRepository;

    public SignalProtocolService(
            SignalProtocolFeatureGuard featureGuard,
            DeviceRepository deviceRepository,
            AccountRepository accountRepository,
            ContactRequestRepository contactRequestRepository,
            SignalDeviceKeyBundleRepository keyBundleRepository,
            SignalOneTimePrekeyRepository oneTimePrekeyRepository,
            SignalKyberPrekeyRepository kyberPrekeyRepository,
            SignalIdentityVerificationRepository verificationRepository
    ) {
        this.featureGuard = featureGuard;
        this.deviceRepository = deviceRepository;
        this.accountRepository = accountRepository;
        this.contactRequestRepository = contactRequestRepository;
        this.keyBundleRepository = keyBundleRepository;
        this.oneTimePrekeyRepository = oneTimePrekeyRepository;
        this.kyberPrekeyRepository = kyberPrekeyRepository;
        this.verificationRepository = verificationRepository;
    }

    @Transactional
    public SignalDeviceDescriptorResponse registerBundle(AuthenticatedDevice caller, RegisterSignalDeviceBundleRequest request) {
        featureGuard.requireEnabled();
        validateUniquePrekeys(request.oneTimePrekeys());
        validateUniqueKyberPrekeys(request.kyberLastResortPrekey(), request.kyberOneTimePrekeys());
        Instant now = Instant.now();
        SignalDeviceKeyBundle existing = keyBundleRepository.findByDeviceId(caller.deviceId()).orElse(null);
        boolean identityChanged = existing != null && !existing.getIdentityKey().equals(request.identityKey());
        if (existing == null) {
            existing = SignalDeviceKeyBundle.register(
                    caller.deviceId(), request.protocolProfile(), request.protocolDeviceId(), request.registrationId(),
                    request.identityKey(), request.signedPrekeyId(), request.signedPrekeyPublic(), request.signedPrekeySignature(),
                    now, request.signedPrekeyExpiresAt(), now
            );
        } else {
            existing.replace(
                    request.protocolProfile(), request.protocolDeviceId(), request.registrationId(), request.identityKey(),
                    request.signedPrekeyId(), request.signedPrekeyPublic(), request.signedPrekeySignature(),
                    now, request.signedPrekeyExpiresAt(), now
            );
        }
        keyBundleRepository.save(existing);

        registerEcOneTimePrekeys(caller.deviceId(), request.oneTimePrekeys(), now);
        registerKyberPrekeys(caller.deviceId(), request.kyberLastResortPrekey(), request.kyberOneTimePrekeys(), now);
        if (identityChanged) {
            verificationRepository.findAllBySubjectDeviceId(caller.deviceId())
                    .forEach(verification -> verification.markChanged(verification.getSafetyNumberFingerprint(), now));
        }
        return descriptor(requireActiveDevice(caller.deviceId()), existing, availableEcPrekeys(caller.deviceId()));
    }

    @Transactional(readOnly = true)
    public List<SignalDeviceDescriptorResponse> listPeerDevices(AuthenticatedDevice caller, String username) {
        featureGuard.requireEnabled();
        Account target = accountRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account was not found."));
        requireBundleAccess(caller.accountId(), target.getId());
        return deviceRepository.findAllByAccountIdAndRevokedAtIsNullOrderByCreatedAtAsc(target.getId()).stream()
                .map(device -> keyBundleRepository.findByDeviceId(device.getId())
                        .filter(bundle -> bundle.getSignedPrekeyExpiresAt().isAfter(Instant.now()))
                        .map(bundle -> descriptor(device, bundle, availableEcPrekeys(device.getId()))))
                .flatMap(java.util.Optional::stream)
                .toList();
    }

    @Transactional
    public SignalPrekeyBundleResponse claimPeerBundle(AuthenticatedDevice caller, UUID targetDeviceId) {
        featureGuard.requireEnabled();
        Device target = requireActiveDevice(targetDeviceId);
        requireBundleAccess(caller.accountId(), target.getAccountId());
        SignalDeviceKeyBundle bundle = keyBundleRepository.findByDeviceId(targetDeviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Signal device bundle was not found."));
        if (!bundle.getSignedPrekeyExpiresAt().isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The target device signed prekey has expired.");
        }

        SignalOneTimePrekey claimedEc = oneTimePrekeyRepository
                .findFirstByDeviceIdAndClaimedAtIsNullOrderByCreatedAtAsc(targetDeviceId)
                .orElse(null);
        UUID ecClaimId = null;
        SignalOneTimePrekeyResponse oneTimePrekey = null;
        if (claimedEc != null) {
            ecClaimId = UUID.randomUUID();
            claimedEc.claim(ecClaimId, Instant.now());
            oneTimePrekey = new SignalOneTimePrekeyResponse(claimedEc.getPrekeyId(), claimedEc.getPublicKey());
        }

        SignalKyberPrekey claimedKyber = kyberPrekeyRepository
                .findFirstByDeviceIdAndLastResortFalseAndClaimedAtIsNullOrderByCreatedAtAsc(targetDeviceId)
                .orElseGet(() -> kyberPrekeyRepository.findFirstByDeviceIdAndLastResortTrueOrderByCreatedAtDesc(targetDeviceId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "The target device has no PQXDH Kyber prekey.")));
        UUID kyberClaimId = null;
        if (!claimedKyber.isLastResort()) {
            kyberClaimId = UUID.randomUUID();
            claimedKyber.claim(kyberClaimId, Instant.now());
        }
        SignalKyberPrekeyResponse kyberPrekey = new SignalKyberPrekeyResponse(
                claimedKyber.getPrekeyId(), claimedKyber.getPublicKey(), claimedKyber.getSignature(), claimedKyber.isLastResort()
        );

        return new SignalPrekeyBundleResponse(
                targetDeviceId, target.getAccountId().toString(), bundle.getProtocolProfile(), bundle.getProtocolDeviceId(),
                bundle.getRegistrationId(), bundle.getIdentityKey(), bundle.getSignedPrekeyId(), bundle.getSignedPrekeyPublic(),
                bundle.getSignedPrekeySignature(), bundle.getSignedPrekeyExpiresAt(), oneTimePrekey, ecClaimId, kyberPrekey,
                kyberClaimId, availableEcPrekeys(targetDeviceId), availableKyberOneTimePrekeys(targetDeviceId)
        );
    }

    @Transactional
    public SignalIdentityVerificationResponse verifyIdentity(
            AuthenticatedDevice caller,
            UUID subjectDeviceId,
            VerifySignalIdentityRequest request
    ) {
        featureGuard.requireEnabled();
        Device subject = requireActiveDevice(subjectDeviceId);
        requireBundleAccess(caller.accountId(), subject.getAccountId());
        if (caller.deviceId().equals(subjectDeviceId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A device cannot verify itself.");
        }
        Instant now = Instant.now();
        SignalIdentityVerification verification = verificationRepository
                .findByVerifierDeviceIdAndSubjectDeviceId(caller.deviceId(), subjectDeviceId)
                .orElseGet(() -> SignalIdentityVerification.create(
                        caller.deviceId(), subjectDeviceId, request.safetyNumberFingerprint(), now
                ));
        verification.verify(request.safetyNumberFingerprint(), now);
        return toResponse(verificationRepository.save(verification));
    }

    @Transactional(readOnly = true)
    public SignalIdentityVerificationResponse getIdentityVerification(AuthenticatedDevice caller, UUID subjectDeviceId) {
        featureGuard.requireEnabled();
        Device subject = requireActiveDevice(subjectDeviceId);
        requireBundleAccess(caller.accountId(), subject.getAccountId());
        return verificationRepository.findByVerifierDeviceIdAndSubjectDeviceId(caller.deviceId(), subjectDeviceId)
                .map(this::toResponse)
                .orElse(new SignalIdentityVerificationResponse(
                        caller.deviceId(), subjectDeviceId, null, SignalIdentityVerificationStatus.UNVERIFIED,
                        null, null, null
                ));
    }

    private void registerEcOneTimePrekeys(UUID deviceId, List<SignalOneTimePrekeyRequest> prekeys, Instant now) {
        for (SignalOneTimePrekeyRequest prekey : prekeys) {
            if (oneTimePrekeyRepository.existsByDeviceIdAndPrekeyId(deviceId, prekey.prekeyId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A Signal one-time prekey identifier may not be reused.");
            }
            oneTimePrekeyRepository.save(SignalOneTimePrekey.create(deviceId, prekey.prekeyId(), prekey.publicKey(), now));
        }
    }

    private void registerKyberPrekeys(
            UUID deviceId,
            SignalKyberPrekeyRequest lastResort,
            List<SignalKyberPrekeyRequest> oneTimePrekeys,
            Instant now
    ) {
        if (kyberPrekeyRepository.existsByDeviceIdAndPrekeyId(deviceId, lastResort.prekeyId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A Signal Kyber prekey identifier may not be reused.");
        }
        kyberPrekeyRepository.save(SignalKyberPrekey.create(
                deviceId, lastResort.prekeyId(), lastResort.publicKey(), lastResort.signature(), true, now
        ));
        for (SignalKyberPrekeyRequest prekey : oneTimePrekeys) {
            if (kyberPrekeyRepository.existsByDeviceIdAndPrekeyId(deviceId, prekey.prekeyId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A Signal Kyber prekey identifier may not be reused.");
            }
            kyberPrekeyRepository.save(SignalKyberPrekey.create(
                    deviceId, prekey.prekeyId(), prekey.publicKey(), prekey.signature(), false, now
            ));
        }
    }

    private void validateUniquePrekeys(List<SignalOneTimePrekeyRequest> prekeys) {
        Set<Long> seen = new HashSet<>();
        for (SignalOneTimePrekeyRequest prekey : prekeys) {
            if (!seen.add(prekey.prekeyId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One-time prekey identifiers must be unique in a bundle upload.");
            }
        }
    }

    private void validateUniqueKyberPrekeys(SignalKyberPrekeyRequest lastResort, List<SignalKyberPrekeyRequest> prekeys) {
        Set<Long> seen = new HashSet<>();
        seen.add(lastResort.prekeyId());
        for (SignalKyberPrekeyRequest prekey : prekeys) {
            if (!seen.add(prekey.prekeyId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kyber prekey identifiers must be unique in a bundle upload.");
            }
        }
    }

    private Device requireActiveDevice(UUID deviceId) {
        return deviceRepository.findByIdAndRevokedAtIsNull(deviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active device was not found."));
    }

    private void requireBundleAccess(UUID callerAccountId, UUID targetAccountId) {
        if (callerAccountId.equals(targetAccountId)) {
            return;
        }
        boolean accepted = contactRequestRepository.existsBySenderAccountIdAndRecipientAccountIdAndStatus(
                callerAccountId, targetAccountId, ContactRequestStatus.ACCEPTED
        ) || contactRequestRepository.existsBySenderAccountIdAndRecipientAccountIdAndStatus(
                targetAccountId, callerAccountId, ContactRequestStatus.ACCEPTED
        );
        if (!accepted) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A Signal bundle is available only to accepted contacts or the same account.");
        }
    }

    private long availableEcPrekeys(UUID deviceId) {
        return oneTimePrekeyRepository.countByDeviceIdAndClaimedAtIsNull(deviceId);
    }

    private long availableKyberOneTimePrekeys(UUID deviceId) {
        return kyberPrekeyRepository.countByDeviceIdAndLastResortFalseAndClaimedAtIsNull(deviceId);
    }

    private SignalDeviceDescriptorResponse descriptor(Device device, SignalDeviceKeyBundle bundle, long availableOneTimePrekeys) {
        return new SignalDeviceDescriptorResponse(
                device.getId(), device.getAccountId().toString(), device.getLabel(), bundle.getProtocolProfile(),
                bundle.getProtocolDeviceId(), bundle.getRegistrationId(), bundle.getSignedPrekeyExpiresAt(), availableOneTimePrekeys
        );
    }

    private SignalIdentityVerificationResponse toResponse(SignalIdentityVerification verification) {
        return new SignalIdentityVerificationResponse(
                verification.getVerifierDeviceId(), verification.getSubjectDeviceId(), verification.getSafetyNumberFingerprint(),
                verification.getStatus(), verification.getVerifiedAt(), verification.getChangedAt(), verification.getUpdatedAt()
        );
    }
}
