package za.hungu.plinth.miniapps;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import za.hungu.plinth.auth.AuthenticatedDevice;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

@Service
public class MiniAppService {
    private final MiniAppFeatureGuard featureGuard;
    private final MiniAppManifestRepository manifestRepository;
    private final MiniAppLaunchTicketRepository launchTicketRepository;
    private final MiniAppSignatureService signatureService;
    private final SecureRandom secureRandom = new SecureRandom();

    public MiniAppService(
            MiniAppFeatureGuard featureGuard,
            MiniAppManifestRepository manifestRepository,
            MiniAppLaunchTicketRepository launchTicketRepository,
            MiniAppSignatureService signatureService
    ) {
        this.featureGuard = featureGuard;
        this.manifestRepository = manifestRepository;
        this.launchTicketRepository = launchTicketRepository;
        this.signatureService = signatureService;
    }

    @Transactional
    public MiniAppManifestResponse register(String registrationKey, RegisterMiniAppManifestRequest request) {
        featureGuard.requireRegistrationKey(registrationKey);
        if (manifestRepository.findByAppIdAndAppVersion(request.appId(), request.appVersion()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This mini-app ID and version are already registered.");
        }
        if (!signatureService.verifyManifest(
                request.appId(), request.appVersion(), request.issuer(), request.origin(), request.permissions(),
                request.publicKeyBase64(), request.signatureBase64()
        )) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The mini-app manifest signature is invalid.");
        }
        MiniAppManifest manifest = manifestRepository.save(MiniAppManifest.register(
                request.appId(), request.appVersion(), request.issuer(), request.origin(), request.publicKeyBase64(),
                request.signatureBase64(), request.permissions(), Instant.now()
        ));
        return toManifestResponse(manifest);
    }

    @Transactional(readOnly = true)
    public MiniAppManifestResponse manifest(String appId, String appVersion) {
        featureGuard.requireEnabled();
        return toManifestResponse(requireManifest(appId, appVersion));
    }

    @Transactional
    public MiniAppLaunchTicketResponse createLaunchTicket(
            AuthenticatedDevice caller,
            String appId,
            String appVersion,
            CreateMiniAppLaunchTicketRequest request
    ) {
        MiniAppProperties properties = featureGuard.configuredProperties();
        MiniAppManifest manifest = requireManifest(appId, appVersion);
        Set<MiniAppPermission> acceptedPermissions = Set.copyOf(request.acceptedPermissions());
        if (!manifest.getPermissions().equals(acceptedPermissions)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Launch consent must match the manifest's declared permissions exactly.");
        }
        UUID ticketId = UUID.randomUUID();
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(properties.ticketLifetimeSeconds());
        String nonce = newNonce();
        String signature = signatureService.signTicket(
                properties.ticketPrivateKeyBase64(), ticketId, manifest.getAppId(), manifest.getAppVersion(),
                caller.accountId(), caller.deviceId(), nonce, expiresAt.getEpochSecond(), manifest.getPermissions()
        );
        MiniAppLaunchTicket ticket = launchTicketRepository.save(MiniAppLaunchTicket.issue(
                ticketId, manifest.getId(), caller.accountId(), caller.deviceId(), nonce, expiresAt, signature, now
        ));
        return toTicketResponse(ticket, manifest, properties, false);
    }

    @Transactional
    public MiniAppLaunchTicketResponse consumeLaunchTicket(AuthenticatedDevice caller, UUID ticketId) {
        MiniAppProperties properties = featureGuard.configuredProperties();
        MiniAppLaunchTicket ticket = launchTicketRepository.findByIdAndAccountIdAndDeviceId(
                        ticketId, caller.accountId(), caller.deviceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mini-app launch ticket was not found."));
        try {
            ticket.consume(Instant.now());
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
        MiniAppManifest manifest = manifestRepository.findById(ticket.getManifestId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mini-app manifest was not found."));
        return toTicketResponse(ticket, manifest, properties, true);
    }

    private MiniAppManifest requireManifest(String appId, String appVersion) {
        return manifestRepository.findByAppIdAndAppVersion(appId, appVersion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mini-app manifest was not found."));
    }

    private String newNonce() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private MiniAppManifestResponse toManifestResponse(MiniAppManifest manifest) {
        return new MiniAppManifestResponse(
                manifest.getId(), manifest.getAppId(), manifest.getAppVersion(), manifest.getIssuer(), manifest.getOrigin(),
                manifest.getPublicKeyBase64(), manifest.getSignatureBase64(), manifest.getPermissions(), manifest.getCreatedAt()
        );
    }

    private MiniAppLaunchTicketResponse toTicketResponse(
            MiniAppLaunchTicket ticket,
            MiniAppManifest manifest,
            MiniAppProperties properties,
            boolean consumed
    ) {
        return new MiniAppLaunchTicketResponse(
                ticket.getId(), manifest.getAppId(), manifest.getAppVersion(), manifest.getOrigin(), ticket.getAccountId(),
                ticket.getDeviceId(), manifest.getPermissions(), ticket.getNonce(), ticket.getExpiresAt(), ticket.getTicketSignatureBase64(),
                properties.ticketPublicKeyBase64(), consumed
        );
    }
}
