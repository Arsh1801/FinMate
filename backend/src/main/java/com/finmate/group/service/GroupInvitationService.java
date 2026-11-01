package com.finmate.group.service;

import com.finmate.common.exception.ApiException;
import com.finmate.common.exception.ErrorCode;
import com.finmate.group.dto.CreateInvitationRequest;
import com.finmate.group.dto.InvitationDto;
import com.finmate.group.dto.RespondInvitationRequest;
import com.finmate.group.entity.*;
import com.finmate.group.repository.ExpenseGroupRepository;
import com.finmate.group.repository.GroupInvitationRepository;
import com.finmate.group.repository.GroupMemberRepository;
import com.finmate.notification.event.GroupInvitationEvent;
import com.finmate.user.entity.User;
import com.finmate.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class GroupInvitationService {

    private final GroupInvitationRepository invitationRepository;
    private final ExpenseGroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final GroupService groupService;
    private final ApplicationEventPublisher eventPublisher;

    public GroupInvitationService(GroupInvitationRepository invitationRepository,
                                  ExpenseGroupRepository groupRepository,
                                  GroupMemberRepository groupMemberRepository,
                                  UserRepository userRepository,
                                  GroupService groupService,
                                  ApplicationEventPublisher eventPublisher) {
        this.invitationRepository = invitationRepository;
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
        this.groupService = groupService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public InvitationDto inviteUserByEmail(Long ownerId, Long groupId, CreateInvitationRequest request) {
        groupService.verifyOwner(groupId, ownerId);

        ExpenseGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> ApiException.notFound("Group not found"));

        if (group.getStatus() == GroupStatus.ARCHIVED) {
            throw ApiException.businessRule(ErrorCode.VALIDATION_FAILED, "Cannot invite to an archived group");
        }

        String email = request.getEmail().trim().toLowerCase();
        User invitee = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.notFound("No registered user found with email '" + email + "'. Users must register first."));

        if (invitee.getId().equals(ownerId)) {
            throw ApiException.businessRule(ErrorCode.INVITATION_INVALID, "You cannot invite yourself");
        }

        if (groupMemberRepository.findActiveMember(groupId, invitee.getId()).isPresent()) {
            throw ApiException.businessRule(ErrorCode.INVITATION_INVALID, "User is already an active member of this group");
        }

        Optional<GroupInvitation> pendingOpt = invitationRepository.findPendingInvitation(groupId, invitee.getId());
        if (pendingOpt.isPresent()) {
            GroupInvitation existing = pendingOpt.get();
            if (existing.getExpiresAt().isAfter(LocalDateTime.now())) {
                throw ApiException.conflict(ErrorCode.VALIDATION_FAILED, "A pending invitation already exists for this user");
            } else {
                existing.setStatus(InvitationStatus.EXPIRED);
                invitationRepository.save(existing);
            }
        }

        User owner = userRepository.findById(ownerId).orElseThrow();

        GroupInvitation invitation = new GroupInvitation();
        invitation.setGroup(group);
        invitation.setInvitedUser(invitee);
        invitation.setInvitedBy(owner);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setCreatedAt(LocalDateTime.now());
        invitation.setExpiresAt(LocalDateTime.now().plusDays(7));

        GroupInvitation saved = invitationRepository.save(invitation);

        // Publish event for notification
        eventPublisher.publishEvent(new GroupInvitationEvent(saved));

        return InvitationDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<InvitationDto> getGroupInvitations(Long ownerId, Long groupId) {
        groupService.verifyOwner(groupId, ownerId);
        return invitationRepository.findByGroupIdOrderByCreatedAtDesc(groupId).stream()
                .map(InvitationDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void cancelInvitation(Long ownerId, Long groupId, Long invitationId) {
        groupService.verifyOwner(groupId, ownerId);

        GroupInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> ApiException.notFound("Invitation not found"));

        if (!invitation.getGroup().getId().equals(groupId)) {
            throw ApiException.notFound("Invitation not found in this group");
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw ApiException.businessRule(ErrorCode.INVITATION_INVALID, "Only pending invitations can be cancelled");
        }

        invitation.setStatus(InvitationStatus.CANCELLED);
        invitation.setRespondedAt(LocalDateTime.now());
        invitationRepository.save(invitation);
    }

    @Transactional(readOnly = true)
    public List<InvitationDto> getMyInvitations(Long userId, InvitationStatus status) {
        InvitationStatus filterStatus = status != null ? status : InvitationStatus.PENDING;
        return invitationRepository.findByInvitedUserIdAndStatusOrderByCreatedAtDesc(userId, filterStatus).stream()
                .map(InvitationDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public InvitationDto respondToInvitation(Long userId, Long invitationId, RespondInvitationRequest request) {
        GroupInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> ApiException.notFound("Invitation not found"));

        if (!invitation.getInvitedUser().getId().equals(userId)) {
            throw ApiException.notFound("Invitation not found");
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw ApiException.businessRule(ErrorCode.INVITATION_INVALID, "Invitation has already been answered or expired");
        }

        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw ApiException.businessRule(ErrorCode.INVITATION_INVALID, "Invitation has expired");
        }

        invitation.setRespondedAt(LocalDateTime.now());

        if ("ACCEPT".equalsIgnoreCase(request.getAction())) {
            invitation.setStatus(InvitationStatus.ACCEPTED);

            // Add or reactivate user as active group member
            ExpenseGroup group = invitation.getGroup();
            Optional<GroupMember> existingMemberOpt = groupMemberRepository.findByGroupIdAndUserId(group.getId(), userId);

            if (existingMemberOpt.isPresent()) {
                GroupMember member = existingMemberOpt.get();
                member.setStatus(MemberStatus.ACTIVE);
                member.setRole(GroupRole.MEMBER);
                member.setJoinedAt(LocalDateTime.now());
                member.setLeftAt(null);
                groupMemberRepository.save(member);
            } else {
                GroupMember member = new GroupMember();
                member.setGroup(group);
                member.setUser(invitation.getInvitedUser());
                member.setRole(GroupRole.MEMBER);
                member.setStatus(MemberStatus.ACTIVE);
                member.setJoinedAt(LocalDateTime.now());
                groupMemberRepository.save(member);
            }
        } else {
            invitation.setStatus(InvitationStatus.REJECTED);
        }

        GroupInvitation saved = invitationRepository.save(invitation);
        return InvitationDto.fromEntity(saved);
    }
}
