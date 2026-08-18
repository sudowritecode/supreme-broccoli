package za.hungu.plinth.contacts;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import za.hungu.plinth.auth.AuthenticatedDevice;
import za.hungu.plinth.auth.DeviceAuthenticator;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contact-requests")
public class ContactController {

    private final DeviceAuthenticator deviceAuthenticator;
    private final ContactService contactService;

    public ContactController(DeviceAuthenticator deviceAuthenticator, ContactService contactService) {
        this.deviceAuthenticator = deviceAuthenticator;
        this.contactService = contactService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContactRequestResponse create(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @Valid @RequestBody CreateContactRequest request
    ) {
        AuthenticatedDevice caller = deviceAuthenticator.require(deviceToken);
        return contactService.create(caller, request);
    }

    @PostMapping("/{requestId}/accept")
    public ContactRequestResponse accept(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @PathVariable UUID requestId
    ) {
        return contactService.accept(deviceAuthenticator.require(deviceToken), requestId);
    }

    @PostMapping("/{requestId}/decline")
    public ContactRequestResponse decline(
            @RequestHeader(name = DeviceAuthenticator.HEADER_NAME, required = false) String deviceToken,
            @PathVariable UUID requestId
    ) {
        return contactService.decline(deviceAuthenticator.require(deviceToken), requestId);
    }
}
