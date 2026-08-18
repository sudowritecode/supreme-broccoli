package za.hungu.plinth.contacts;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import za.hungu.plinth.auth.AuthenticatedDevice;
import za.hungu.plinth.conversation.Conversation;
import za.hungu.plinth.conversation.DirectConversationService;
import za.hungu.plinth.identity.Account;
import za.hungu.plinth.identity.AccountRepository;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class ContactService {

    private final AccountRepository accountRepository;
    private final ContactRequestRepository contactRequestRepository;
    private final DirectConversationService directConversationService;

    public ContactService(
            AccountRepository accountRepository,
            ContactRequestRepository contactRequestRepository,
            DirectConversationService directConversationService
    ) {
        this.accountRepository = accountRepository;
        this.contactRequestRepository = contactRequestRepository;
        this.directConversationService = directConversationService;
    }

    @Transactional
    public ContactRequestResponse create(AuthenticatedDevice caller, CreateContactRequest request) {
        String recipientUsername = request.recipientUsername().trim().toLowerCase(Locale.ROOT);
        Account recipient = accountRepository.findByUsername(recipientUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient account was not found."));

        if (caller.accountId().equals(recipient.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A contact request cannot target the same account.");
        }

        ContactRequest existing = contactRequestRepository
                .findBySenderAccountIdAndRecipientAccountId(caller.accountId(), recipient.getId())
                .orElse(null);
        if (existing != null) {
            return toResponse(existing, conversationForAccepted(existing));
        }

        ContactRequest reverse = contactRequestRepository
                .findBySenderAccountIdAndRecipientAccountId(recipient.getId(), caller.accountId())
                .orElse(null);
        if (reverse != null && reverse.getStatus() == ContactRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An incoming contact request is already awaiting a response.");
        }
        if (reverse != null && reverse.getStatus() == ContactRequestStatus.ACCEPTED) {
            Conversation conversation = directConversationService.findOrCreate(caller.accountId(), recipient.getId(), Instant.now());
            return toResponse(reverse, conversation.getId());
        }

        ContactRequest contactRequest = contactRequestRepository.save(
                ContactRequest.create(caller.accountId(), recipient.getId(), Instant.now())
        );
        return toResponse(contactRequest, null);
    }

    @Transactional
    public ContactRequestResponse accept(AuthenticatedDevice caller, UUID requestId) {
        ContactRequest contactRequest = contactRequestRepository.findByIdAndRecipientAccountId(requestId, caller.accountId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact request was not found."));
        contactRequest.accept(Instant.now());
        Conversation conversation = directConversationService.findOrCreate(
                contactRequest.getSenderAccountId(),
                contactRequest.getRecipientAccountId(),
                Instant.now()
        );
        return toResponse(contactRequest, conversation.getId());
    }

    @Transactional
    public ContactRequestResponse decline(AuthenticatedDevice caller, UUID requestId) {
        ContactRequest contactRequest = contactRequestRepository.findByIdAndRecipientAccountId(requestId, caller.accountId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact request was not found."));
        contactRequest.decline(Instant.now());
        return toResponse(contactRequest, null);
    }

    public boolean areConnected(UUID firstAccountId, UUID secondAccountId) {
        return contactRequestRepository.existsBySenderAccountIdAndRecipientAccountIdAndStatus(
                firstAccountId,
                secondAccountId,
                ContactRequestStatus.ACCEPTED
        ) || contactRequestRepository.existsBySenderAccountIdAndRecipientAccountIdAndStatus(
                secondAccountId,
                firstAccountId,
                ContactRequestStatus.ACCEPTED
        );
    }

    private UUID conversationForAccepted(ContactRequest contactRequest) {
        if (contactRequest.getStatus() != ContactRequestStatus.ACCEPTED) {
            return null;
        }
        return directConversationService.findOrCreate(
                contactRequest.getSenderAccountId(),
                contactRequest.getRecipientAccountId(),
                Instant.now()
        ).getId();
    }

    private ContactRequestResponse toResponse(ContactRequest contactRequest, UUID conversationId) {
        return new ContactRequestResponse(
                contactRequest.getId(),
                contactRequest.getSenderAccountId(),
                contactRequest.getRecipientAccountId(),
                contactRequest.getStatus(),
                contactRequest.getCreatedAt(),
                contactRequest.getRespondedAt(),
                conversationId
        );
    }
}
