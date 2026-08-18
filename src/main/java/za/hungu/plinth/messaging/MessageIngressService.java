package za.hungu.plinth.messaging;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import za.hungu.plinth.api.SendEncryptedMessageRequest;
import za.hungu.plinth.auth.AuthenticatedDevice;
import za.hungu.plinth.conversation.ConversationMemberRepository;
import za.hungu.plinth.conversation.ConversationMemberStatus;
import za.hungu.plinth.identity.Device;
import za.hungu.plinth.identity.DeviceRepository;
import za.hungu.plinth.outbox.EncryptedMessageQueued;
import za.hungu.plinth.outbox.MessageOutbox;
import za.hungu.plinth.outbox.MessageOutboxRepository;

import java.time.Instant;

@Service
public class MessageIngressService {

    private final DeviceRepository deviceRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final MessageOutboxRepository messageOutboxRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public MessageIngressService(
            DeviceRepository deviceRepository,
            ConversationMemberRepository conversationMemberRepository,
            MessageOutboxRepository messageOutboxRepository,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        this.deviceRepository = deviceRepository;
        this.conversationMemberRepository = conversationMemberRepository;
        this.messageOutboxRepository = messageOutboxRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public MessageIngressResult queue(AuthenticatedDevice sender, SendEncryptedMessageRequest request) {
        MessageOutbox existing = messageOutboxRepository.findByMessageId(request.messageId()).orElse(null);
        if (existing != null) {
            if (sameEnvelope(existing, sender, request)) {
                return new MessageIngressResult(existing.getMessageId(), existing.getReceivedAt(), true);
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The message identifier has already been used.");
        }

        Device recipient = deviceRepository.findByIdAndRevokedAtIsNull(request.recipientDeviceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient device was not found."));
        if (recipient.getId().equals(sender.deviceId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A message cannot target the same device.");
        }

        boolean senderIsMember = conversationMemberRepository.existsByConversationIdAndAccountIdAndMemberStatus(
                request.conversationId(), sender.accountId(), ConversationMemberStatus.ACTIVE
        );
        boolean recipientIsMember = conversationMemberRepository.existsByConversationIdAndAccountIdAndMemberStatus(
                request.conversationId(), recipient.getAccountId(), ConversationMemberStatus.ACTIVE
        );
        if (!senderIsMember || !recipientIsMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Both accounts must be active conversation members.");
        }

        Instant receivedAt = Instant.now();
        MessageOutbox outbox = messageOutboxRepository.save(MessageOutbox.queue(
                request.messageId(),
                request.conversationId(),
                sender.deviceId(),
                recipient.getId(),
                request.ciphertext(),
                receivedAt
        ));
        applicationEventPublisher.publishEvent(new EncryptedMessageQueued(outbox.getId()));
        return new MessageIngressResult(outbox.getMessageId(), receivedAt, false);
    }

    private boolean sameEnvelope(
            MessageOutbox existing,
            AuthenticatedDevice sender,
            SendEncryptedMessageRequest request
    ) {
        return existing.getConversationId().equals(request.conversationId())
                && existing.getSenderDeviceId().equals(sender.deviceId())
                && existing.getRecipientDeviceId().equals(request.recipientDeviceId())
                && existing.getCiphertext().equals(request.ciphertext());
    }
}
