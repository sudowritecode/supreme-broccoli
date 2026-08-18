package za.hungu.plinth.identity;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import za.hungu.plinth.auth.DeviceTokenService;

import java.time.Instant;
import java.util.Locale;

@Service
public class RegistrationService {

    private final AccountRepository accountRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceTokenService deviceTokenService;

    public RegistrationService(
            AccountRepository accountRepository,
            DeviceRepository deviceRepository,
            DeviceTokenService deviceTokenService
    ) {
        this.accountRepository = accountRepository;
        this.deviceRepository = deviceRepository;
        this.deviceTokenService = deviceTokenService;
    }

    @Transactional
    public RegisterAccountResponse register(RegisterAccountRequest request) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        if (accountRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is unavailable.");
        }

        Instant now = Instant.now();
        DeviceTokenService.IssuedToken issuedToken = deviceTokenService.issue();
        Account account = accountRepository.save(Account.create(username, now));
        Device device = deviceRepository.save(Device.create(
                account.getId(),
                request.publicIdentityKey().trim(),
                issuedToken.hash(),
                request.deviceLabel().trim(),
                now
        ));

        return new RegisterAccountResponse(
                account.getId(),
                account.getUsername(),
                device.getId(),
                issuedToken.rawValue(),
                now
        );
    }
}
