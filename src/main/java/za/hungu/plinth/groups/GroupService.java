package za.hungu.plinth.groups;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import za.hungu.plinth.auth.AuthenticatedDevice;
import za.hungu.plinth.conversation.Conversation;
import za.hungu.plinth.conversation.ConversationMember;
import za.hungu.plinth.conversation.ConversationMemberRepository;
import za.hungu.plinth.conversation.ConversationMemberRole;
import za.hungu.plinth.conversation.ConversationMemberStatus;
import za.hungu.plinth.conversation.ConversationRepository;
import za.hungu.plinth.conversation.ConversationType;
import za.hungu.plinth.identity.Account;
import za.hungu.plinth.identity.AccountRepository;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class GroupService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final AccountRepository accountRepository;

    public GroupService(
            ConversationRepository conversationRepository,
            ConversationMemberRepository memberRepository,
            AccountRepository accountRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.memberRepository = memberRepository;
        this.accountRepository = accountRepository;
    }

    public GroupResponse create(AuthenticatedDevice creator, CreateGroupRequest request) {
        Conversation group = conversationRepository.save(Conversation.group(request.name().trim(), Instant.now()));
        long membershipVersion = group.incrementMembershipVersion();
        ConversationMember owner = ConversationMember.active(
                group.getId(), creator.accountId(), ConversationMemberRole.OWNER, membershipVersion, Instant.now()
        );
        memberRepository.save(owner);
        return new GroupResponse(group.getId(), group.getName(), group.getMembershipVersion(), owner.getRole(), owner.getMemberStatus());
    }

    public GroupMemberResponse invite(AuthenticatedDevice actor, UUID groupId, GroupInvitationRequest request) {
        Conversation group = requireGroup(groupId);
        ConversationMember actorMember = requireActiveMember(groupId, actor.accountId());
        if (!canInvite(actorMember.getRole())) {
            throw forbidden("Only a group owner or admin can invite members.");
        }
        Account invitee = accountRepository.findByUsername(normalizeUsername(request.username()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitee account was not found."));
        if (invitee.getId().equals(actor.accountId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A member cannot invite themselves.");
        }
        ConversationMember existing = memberRepository.findByConversationIdAndAccountId(groupId, invitee.getId()).orElse(null);
        if (existing != null) {
            if (existing.getMemberStatus() == ConversationMemberStatus.INVITED) {
                return toMemberResponse(existing);
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This account already has group membership history.");
        }
        long membershipVersion = group.incrementMembershipVersion();
        ConversationMember invite = ConversationMember.invited(groupId, invitee.getId(), membershipVersion, Instant.now());
        memberRepository.save(invite);
        return toMemberResponse(invite);
    }

    public GroupMemberResponse accept(AuthenticatedDevice actor, UUID groupId) {
        Conversation group = requireGroup(groupId);
        ConversationMember invite = requireMember(groupId, actor.accountId());
        if (invite.getMemberStatus() != ConversationMemberStatus.INVITED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No pending group invitation exists for this device account.");
        }
        invite.accept(group.incrementMembershipVersion(), Instant.now());
        return toMemberResponse(invite);
    }

    public GroupMemberResponse decline(AuthenticatedDevice actor, UUID groupId) {
        Conversation group = requireGroup(groupId);
        ConversationMember invite = requireMember(groupId, actor.accountId());
        if (invite.getMemberStatus() != ConversationMemberStatus.INVITED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No pending group invitation exists for this device account.");
        }
        invite.decline(group.incrementMembershipVersion(), Instant.now());
        return toMemberResponse(invite);
    }

    public GroupMemberResponse leave(AuthenticatedDevice actor, UUID groupId) {
        Conversation group = requireGroup(groupId);
        ConversationMember member = requireActiveMember(groupId, actor.accountId());
        if (member.getRole() == ConversationMemberRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The group owner must transfer ownership before leaving.");
        }
        member.leave(group.incrementMembershipVersion(), Instant.now());
        return toMemberResponse(member);
    }

    public GroupMemberResponse remove(AuthenticatedDevice actor, UUID groupId, UUID targetAccountId) {
        Conversation group = requireGroup(groupId);
        ConversationMember actorMember = requireActiveMember(groupId, actor.accountId());
        ConversationMember targetMember = requireActiveMember(groupId, targetAccountId);
        if (!canRemove(actorMember.getRole(), targetMember.getRole())) {
            throw forbidden("The caller cannot remove this group member.");
        }
        targetMember.remove(group.incrementMembershipVersion(), Instant.now());
        return toMemberResponse(targetMember);
    }

    private Conversation requireGroup(UUID groupId) {
        Conversation conversation = conversationRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group conversation was not found."));
        if (conversation.getType() != ConversationType.GROUP) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The conversation is not a private group.");
        }
        return conversation;
    }

    private ConversationMember requireActiveMember(UUID groupId, UUID accountId) {
        ConversationMember member = requireMember(groupId, accountId);
        if (!member.isActive()) {
            throw forbidden("The account is not an active group member.");
        }
        return member;
    }

    private ConversationMember requireMember(UUID groupId, UUID accountId) {
        return memberRepository.findByConversationIdAndAccountId(groupId, accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group member was not found."));
    }

    private boolean canInvite(ConversationMemberRole role) {
        return role == ConversationMemberRole.OWNER || role == ConversationMemberRole.ADMIN;
    }

    private boolean canRemove(ConversationMemberRole actorRole, ConversationMemberRole targetRole) {
        if (actorRole == ConversationMemberRole.OWNER) {
            return targetRole != ConversationMemberRole.OWNER;
        }
        return actorRole == ConversationMemberRole.ADMIN && targetRole == ConversationMemberRole.MEMBER;
    }

    private GroupMemberResponse toMemberResponse(ConversationMember member) {
        return new GroupMemberResponse(
                member.getConversationId(),
                member.getAccountId(),
                member.getRole(),
                member.getMemberStatus(),
                member.getMembershipVersion()
        );
    }

    private ResponseStatusException forbidden(String message) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, message);
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
