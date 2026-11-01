package com.finmate.group.service;

import com.finmate.common.exception.ApiException;
import com.finmate.common.exception.ErrorCode;
import com.finmate.common.util.MoneyUtils;
import com.finmate.expense.entity.SharedExpense;
import com.finmate.expense.repository.SharedExpenseRepository;
import com.finmate.group.dto.CreateGroupRequest;
import com.finmate.group.dto.GroupActivityDto;
import com.finmate.group.dto.GroupDto;
import com.finmate.group.dto.GroupMemberDto;
import com.finmate.group.dto.UpdateGroupRequest;
import com.finmate.group.entity.*;
import com.finmate.group.repository.ExpenseGroupRepository;
import com.finmate.group.repository.GroupMemberRepository;
import com.finmate.settlement.dto.GroupBalancesDto;
import com.finmate.settlement.dto.MemberBalanceDto;
import com.finmate.settlement.entity.Settlement;
import com.finmate.settlement.repository.SettlementRepository;
import com.finmate.settlement.service.BalanceEngine;
import com.finmate.user.entity.User;
import com.finmate.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GroupService {

    private final ExpenseGroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final SharedExpenseRepository sharedExpenseRepository;
    private final SettlementRepository settlementRepository;
    private final BalanceEngine balanceEngine;

    public GroupService(ExpenseGroupRepository groupRepository,
                        GroupMemberRepository groupMemberRepository,
                        UserRepository userRepository,
                        SharedExpenseRepository sharedExpenseRepository,
                        SettlementRepository settlementRepository,
                        BalanceEngine balanceEngine) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
        this.sharedExpenseRepository = sharedExpenseRepository;
        this.settlementRepository = settlementRepository;
        this.balanceEngine = balanceEngine;
    }

    @Transactional(readOnly = true)
    public List<GroupDto> getUserGroups(Long userId, GroupStatus status) {
        List<ExpenseGroup> groups = groupRepository.findUserGroups(userId, status);
        List<GroupDto> dtos = new ArrayList<>();

        for (ExpenseGroup group : groups) {
            GroupDto dto = mapToGroupDto(group);
            // Calculate my balance
            try {
                GroupBalancesDto balances = balanceEngine.computeGroupBalances(group.getId());
                for (MemberBalanceDto mb : balances.getMembers()) {
                    if (mb.getUserId().equals(userId)) {
                        dto.setMyBalance(mb.getNetBalance());
                        break;
                    }
                }
            } catch (Exception ignored) {
                dto.setMyBalance("0.00");
            }
            dtos.add(dto);
        }

        return dtos;
    }

    @Transactional(readOnly = true)
    public GroupDto getGroupDetail(Long userId, Long groupId) {
        verifyActiveMember(groupId, userId);

        ExpenseGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> ApiException.notFound("Group not found"));

        GroupDto dto = mapToGroupDto(group);
        try {
            GroupBalancesDto balances = balanceEngine.computeGroupBalances(group.getId());
            for (MemberBalanceDto mb : balances.getMembers()) {
                if (mb.getUserId().equals(userId)) {
                    dto.setMyBalance(mb.getNetBalance());
                    break;
                }
            }
        } catch (Exception ignored) {
            dto.setMyBalance("0.00");
        }
        return dto;
    }

    @Transactional
    public GroupDto createGroup(Long userId, CreateGroupRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));

        String name = request.getName().trim();
        groupRepository.findActiveByOwnerAndName(userId, name).ifPresent(g -> {
            throw ApiException.conflict(ErrorCode.VALIDATION_FAILED, "You already have an active group named '" + name + "'");
        });

        ExpenseGroup group = new ExpenseGroup();
        group.setName(name);
        group.setDescription(request.getDescription());
        group.setCreatedBy(user);
        group.setStatus(GroupStatus.ACTIVE);

        ExpenseGroup savedGroup = groupRepository.save(group);

        // Creator becomes OWNER and first ACTIVE member
        GroupMember ownerMember = new GroupMember();
        ownerMember.setGroup(savedGroup);
        ownerMember.setUser(user);
        ownerMember.setRole(GroupRole.OWNER);
        ownerMember.setStatus(MemberStatus.ACTIVE);
        ownerMember.setJoinedAt(LocalDateTime.now());
        groupMemberRepository.save(ownerMember);

        GroupDto dto = mapToGroupDto(savedGroup);
        dto.setMemberCount(1);
        dto.setMyBalance("0.00");
        return dto;
    }

    @Transactional
    public GroupDto updateGroup(Long userId, Long groupId, UpdateGroupRequest request) {
        verifyOwner(groupId, userId);

        ExpenseGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> ApiException.notFound("Group not found"));

        if (request.getVersion() != null && !group.getVersion().equals(request.getVersion())) {
            throw ApiException.conflict(ErrorCode.CONCURRENT_MODIFICATION,
                    "Group was modified by another session. Please refresh and retry.");
        }

        String name = request.getName().trim();
        groupRepository.findActiveByOwnerAndName(userId, name).ifPresent(dup -> {
            if (!dup.getId().equals(groupId)) {
                throw ApiException.conflict(ErrorCode.VALIDATION_FAILED, "You already have an active group named '" + name + "'");
            }
        });

        group.setName(name);
        group.setDescription(request.getDescription());
        ExpenseGroup saved = groupRepository.save(group);

        return mapToGroupDto(saved);
    }

    @Transactional
    public GroupDto archiveGroup(Long userId, Long groupId) {
        verifyOwner(groupId, userId);

        ExpenseGroup group = groupRepository.findByIdWithLock(groupId)
                .orElseThrow(() -> ApiException.notFound("Group not found"));

        // All net balances must be 0.00 (BR-114)
        GroupBalancesDto balances = balanceEngine.computeGroupBalances(groupId);
        for (MemberBalanceDto mb : balances.getMembers()) {
            if (mb.getNetDecimal().compareTo(BigDecimal.ZERO) != 0) {
                throw ApiException.businessRule(ErrorCode.BALANCE_NOT_ZERO,
                        "Cannot archive group until all balances are settled to ₹0.00");
            }
        }

        group.setStatus(GroupStatus.ARCHIVED);
        ExpenseGroup saved = groupRepository.save(group);
        return mapToGroupDto(saved);
    }

    @Transactional(readOnly = true)
    public List<GroupMemberDto> getMembers(Long userId, Long groupId) {
        verifyActiveMember(groupId, userId);

        List<GroupMember> members = groupMemberRepository.findByGroupIdOrderByJoinedAtAsc(groupId);
        GroupBalancesDto balances = balanceEngine.computeGroupBalances(groupId);

        Map<Long, BigDecimal> netMap = new HashMap<>();
        for (MemberBalanceDto mb : balances.getMembers()) {
            netMap.put(mb.getUserId(), mb.getNetDecimal());
        }

        return members.stream().map(m ->
                GroupMemberDto.fromEntity(m, netMap.getOrDefault(m.getUser().getId(), BigDecimal.ZERO))
        ).collect(Collectors.toList());
    }

    @Transactional
    public void removeMember(Long userId, Long groupId, Long targetUserId) {
        verifyOwner(groupId, userId);

        if (userId.equals(targetUserId)) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Owner cannot remove themselves. Transfer ownership first.");
        }

        GroupMember target = groupMemberRepository.findActiveMember(groupId, targetUserId)
                .orElseThrow(() -> ApiException.notFound("Member not found in group"));

        // Target member's net balance must be 0.00 (BR-113)
        GroupBalancesDto balances = balanceEngine.computeGroupBalances(groupId);
        for (MemberBalanceDto mb : balances.getMembers()) {
            if (mb.getUserId().equals(targetUserId) && mb.getNetDecimal().compareTo(BigDecimal.ZERO) != 0) {
                throw ApiException.businessRule(ErrorCode.BALANCE_NOT_ZERO,
                        "Member has an unsettled balance (" + mb.getNetBalance() + ") and cannot be removed");
            }
        }

        target.setStatus(MemberStatus.REMOVED);
        target.setLeftAt(LocalDateTime.now());
        groupMemberRepository.save(target);
    }

    @Transactional
    public void leaveGroup(Long userId, Long groupId) {
        GroupMember member = groupMemberRepository.findActiveMember(groupId, userId)
                .orElseThrow(() -> ApiException.notFound("You are not an active member of this group"));

        if (member.getRole() == GroupRole.OWNER) {
            long otherActiveCount = groupMemberRepository.findActiveMembers(groupId).stream()
                    .filter(m -> !m.getUser().getId().equals(userId)).count();
            if (otherActiveCount > 0) {
                throw ApiException.businessRule(ErrorCode.BALANCE_NOT_ZERO,
                        "Owner must transfer ownership before leaving the group");
            }
        }

        // Net balance must be 0.00 and no pending settlements (BR-112)
        GroupBalancesDto balances = balanceEngine.computeGroupBalances(groupId);
        for (MemberBalanceDto mb : balances.getMembers()) {
            if (mb.getUserId().equals(userId) && mb.getNetDecimal().compareTo(BigDecimal.ZERO) != 0) {
                throw ApiException.businessRule(ErrorCode.BALANCE_NOT_ZERO,
                        "Cannot leave group with unsettled balance (" + mb.getNetBalance() + ")");
            }
        }

        if (settlementRepository.hasPendingSettlementForUser(groupId, userId)) {
            throw ApiException.businessRule(ErrorCode.BALANCE_NOT_ZERO,
                    "Cannot leave group while you have pending settlements awaiting confirmation");
        }

        member.setStatus(MemberStatus.LEFT);
        member.setLeftAt(LocalDateTime.now());
        groupMemberRepository.save(member);
    }

    @Transactional
    public void transferOwnership(Long userId, Long groupId, Long newOwnerId) {
        verifyOwner(groupId, userId);

        GroupMember currentOwner = groupMemberRepository.findActiveMember(groupId, userId)
                .orElseThrow(() -> ApiException.notFound("Owner not found"));

        GroupMember newOwner = groupMemberRepository.findActiveMember(groupId, newOwnerId)
                .orElseThrow(() -> ApiException.businessRule(ErrorCode.MEMBER_NOT_ACTIVE, "Target user is not an active member of this group"));

        currentOwner.setRole(GroupRole.MEMBER);
        newOwner.setRole(GroupRole.OWNER);

        groupMemberRepository.save(currentOwner);
        groupMemberRepository.save(newOwner);

        ExpenseGroup group = groupRepository.findById(groupId).orElseThrow();
        group.setCreatedBy(newOwner.getUser());
        groupRepository.save(group);
    }

    @Transactional(readOnly = true)
    public List<GroupActivityDto> getGroupHistory(Long userId, Long groupId, String type, Long memberId) {
        verifyActiveMember(groupId, userId);

        List<GroupActivityDto> activities = new ArrayList<>();

        // Shared expenses
        if (type == null || "EXPENSE".equalsIgnoreCase(type)) {
            List<SharedExpense> expenses = sharedExpenseRepository.findAllByGroupId(groupId);
            for (SharedExpense exp : expenses) {
                if (memberId != null && !exp.getPaidBy().getId().equals(memberId)) continue;
                String statusText = exp.getDeletedAt() != null ? " (Deleted)" : "";
                activities.add(new GroupActivityDto(
                        "exp_" + exp.getId(),
                        "EXPENSE",
                        exp.getDescription() + statusText,
                        "Paid by " + exp.getPaidBy().getFullName() + " (" + exp.getSplitType() + " split)",
                        exp.getPaidBy().getId(),
                        exp.getPaidBy().getFullName(),
                        MoneyUtils.formatIndianCurrency(exp.getAmount()),
                        exp.getCreatedAt()
                ));
            }
        }

        // Settlements
        if (type == null || "SETTLEMENT".equalsIgnoreCase(type)) {
            List<Settlement> settlements = settlementRepository.findByGroupIdOrderByDateDesc(groupId);
            for (Settlement s : settlements) {
                if (memberId != null && !s.getFromUser().getId().equals(memberId) && !s.getToUser().getId().equals(memberId)) continue;
                activities.add(new GroupActivityDto(
                        "stl_" + s.getId(),
                        "SETTLEMENT",
                        "Settlement: " + s.getFromUser().getFullName() + " → " + s.getToUser().getFullName(),
                        "Status: " + s.getStatus() + (s.getNote() != null ? " (" + s.getNote() + ")" : ""),
                        s.getRecordedBy().getId(),
                        s.getRecordedBy().getFullName(),
                        MoneyUtils.formatIndianCurrency(s.getAmount()),
                        s.getCreatedAt()
                ));
            }
        }

        // Membership
        if (type == null || "MEMBERSHIP".equalsIgnoreCase(type)) {
            List<GroupMember> members = groupMemberRepository.findByGroupIdOrderByJoinedAtAsc(groupId);
            for (GroupMember m : members) {
                if (memberId != null && !m.getUser().getId().equals(memberId)) continue;
                activities.add(new GroupActivityDto(
                        "mem_" + m.getId(),
                        "MEMBERSHIP",
                        m.getUser().getFullName() + " joined the group",
                        "Role: " + m.getRole() + ", Status: " + m.getStatus(),
                        m.getUser().getId(),
                        m.getUser().getFullName(),
                        null,
                        m.getJoinedAt()
                ));
            }
        }

        activities.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        return activities;
    }

    public void verifyActiveMember(Long groupId, Long userId) {
        groupMemberRepository.findActiveMember(groupId, userId)
                .orElseThrow(() -> ApiException.notFound("Group not found or you are not an active member"));
    }

    public void verifyOwner(Long groupId, Long userId) {
        GroupMember member = groupMemberRepository.findActiveMember(groupId, userId)
                .orElseThrow(() -> ApiException.notFound("Group not found or you are not an active member"));
        if (member.getRole() != GroupRole.OWNER) {
            throw ApiException.forbidden("Only group owners can perform this operation");
        }
    }

    private GroupDto mapToGroupDto(ExpenseGroup group) {
        GroupDto dto = new GroupDto();
        dto.setId(group.getId());
        dto.setName(group.getName());
        dto.setDescription(group.getDescription());
        dto.setCreatedById(group.getCreatedBy().getId());
        dto.setCreatedByName(group.getCreatedBy().getFullName());
        dto.setStatus(group.getStatus());
        dto.setMemberCount(groupMemberRepository.findActiveMembers(group.getId()).size());
        dto.setCreatedAt(group.getCreatedAt());
        dto.setVersion(group.getVersion());
        return dto;
    }
}
