package za.hungu.plinth.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.hungu.plinth.identity.Device;
import za.hungu.plinth.identity.DeviceRepository;
import za.hungu.plinth.realtime.DeliveryWebSocketRegistry;

import java.time.Instant;
import java.util.UUID;

@Service
public class DeviceRevocationService {

    private final DeviceRepository deviceRepository;
    private final DeviceSessionService deviceSessionService;
    private final DeliveryWebSocketRegistry webSocketRegistry;

    public DeviceRevocationService(
            DeviceRepository deviceRepository,
            DeviceSessionService deviceSessionService,
            DeliveryWebSocketRegistry webSocketRegistry
    ) {
        this.deviceRepository = deviceRepository;
        this.deviceSessionService = deviceSessionService;
        this.webSocketRegistry = webSocketRegistry;
    }

    @Transactional
    public void revokeOwnedDevice(AuthenticatedDevice authenticatedDevice, UUID deviceId) {
        Device device = deviceRepository.findByIdAndAccountIdAndRevokedAtIsNull(deviceId, authenticatedDevice.accountId())
                .orElseThrow(() -> new IllegalArgumentException("Active device was not found for this account."));
        Instant revokedAt = Instant.now();
        device.revoke(revokedAt);
        deviceSessionService.revokeForDevice(deviceId);
        webSocketRegistry.closeDevice(deviceId);
    }
}
