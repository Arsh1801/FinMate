package com.finmate.expense.service;

import com.finmate.category.entity.Category;
import com.finmate.category.repository.CategoryRepository;
import com.finmate.common.exception.ApiException;
import com.finmate.common.exception.ErrorCode;
import com.finmate.common.util.DateUtils;
import com.finmate.common.util.MoneyUtils;
import com.finmate.expense.dto.*;
import com.finmate.expense.entity.*;
import com.finmate.expense.repository.ExpenseSplitRepository;
import com.finmate.expense.repository.SharedExpenseRepository;
import com.finmate.expense.split.CalculatedSplitResult;
import com.finmate.expense.split.SplitEngine;
import com.finmate.expense.split.SplitItemInput;
import com.finmate.group.entity.ExpenseGroup;
import com.finmate.group.entity.GroupMember;
import com.finmate.group.entity.GroupRole;
import com.finmate.group.entity.GroupStatus;
import com.finmate.group.repository.ExpenseGroupRepository;
import com.finmate.group.repository.GroupMemberRepository;
import com.finmate.group.service.GroupService;
import com.finmate.notification.event.GroupExpenseChangedEvent;
import com.finmate.settlement.repository.SettlementRepository;
import com.finmate.settlement.service.BalanceEngine;
import com.finmate.user.entity.User;
import com.finmate.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SharedExpenseService {

    private final SharedExpenseRepository sharedExpenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final ExpenseGroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final SettlementRepository settlementRepository;
    private final SplitEngine splitEngine;
    private final BalanceEngine balanceEngine;
    private final GroupService groupService;
    private final ApplicationEventPublisher eventPublisher;

    public SharedExpenseService(SharedExpenseRepository sharedExpenseRepository,
                                ExpenseSplitRepository expenseSplitRepository,
                                ExpenseGroupRepository groupRepository,
                                GroupMemberRepository groupMemberRepository,
                                UserRepository userRepository,
                                CategoryRepository categoryRepository,
                                SettlementRepository settlementRepository,
                                SplitEngine splitEngine,
                                BalanceEngine balanceEngine,
                                GroupService groupService,
                                ApplicationEventPublisher eventPublisher) {
        this.sharedExpenseRepository = sharedExpenseRepository;
        this.expenseSplitRepository = expenseSplitRepository;
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.settlementRepository = settlementRepository;
        this.splitEngine = splitEngine;
        this.balanceEngine = balanceEngine;
        this.groupService = groupService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public SharedExpenseDto createExpense(Long userId, Long groupId, CreateSharedExpenseRequest request) {
        groupService.verifyActiveMember(groupId, userId);

        // BR-042: acquire pessimistic lock on the group row
        ExpenseGroup group = groupRepository.findByIdWithLock(groupId)
                .orElseThrow(() -> ApiException.notFound("Group not found"));

        if (group.getStatus() == GroupStatus.ARCHIVED) {
            throw ApiException.businessRule(ErrorCode.VALIDATION_FAILED, "Cannot add expenses to an archived group");
        }

        // Group size validation: must have at least 2 active members (page 24)
        List<GroupMember> activeMembers = groupMemberRepository.findActiveMembers(groupId);
        if (activeMembers.size() < 2) {
            throw ApiException.businessRule(ErrorCode.VALIDATION_FAILED, "A group must have at least 2 members to record expenses");
        }

        Set<Long> activeMemberIds = activeMembers.stream()
                .map(m -> m.getUser().getId())
                .collect(Collectors.toSet());

        // Validate payer is active member
        if (!activeMemberIds.contains(request.getPaidBy())) {
            throw ApiException.businessRule(ErrorCode.MEMBER_NOT_ACTIVE, "Payer is not an active member of this group");
        }

        // Validate participants are distinct active members
        Set<Long> participantIds = new HashSet<>();
        List<SplitItemInput> inputs = new ArrayList<>();
        for (SplitItemRequest splitReq : request.getSplits()) {
            if (!activeMemberIds.contains(splitReq.getUserId())) {
                throw ApiException.businessRule(ErrorCode.MEMBER_NOT_ACTIVE, "Participant " + splitReq.getUserId() + " is not an active member of this group");
            }
            if (!participantIds.add(splitReq.getUserId())) {
                throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Duplicate participant in split list: " + splitReq.getUserId());
            }
            inputs.add(new SplitItemInput(splitReq.getUserId(), splitReq.getAmount(), splitReq.getPercentage()));
        }

        BigDecimal amount = MoneyUtils.validateAndScale(request.getAmount());
        if (request.getExpenseDate().isAfter(DateUtils.today())) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Expense date cannot be in the future");
        }

        User payer = userRepository.findById(request.getPaidBy()).orElseThrow();
        User creator = userRepository.findById(userId).orElseThrow();

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId()).orElse(null);
        }

        // Calculate splits via strategy pattern
        List<CalculatedSplitResult> splitResults = splitEngine.computeSplits(request.getSplitType(), amount, inputs);

        SharedExpense expense = new SharedExpense();
        expense.setGroup(group);
        expense.setPaidBy(payer);
        expense.setAmount(amount);
        expense.setDescription(request.getDescription().trim());
        expense.setNotes(request.getNotes());
        expense.setExpenseDate(request.getExpenseDate());
        expense.setCategory(category);
        expense.setSplitType(request.getSplitType());
        expense.setCreatedBy(creator);

        for (CalculatedSplitResult res : splitResults) {
            User splitUser = userRepository.findById(res.getUserId()).orElseThrow();
            expense.addSplit(new ExpenseSplit(expense, splitUser, res.getShareAmount(), res.getPercentage()));
        }

        SharedExpense saved = sharedExpenseRepository.save(expense);

        // Verify balance invariant: sum of net balances == 0.00
        balanceEngine.computeGroupBalances(groupId);

        // Publish event for notification (Observer Pattern)
        List<Long> affectedUserIds = new ArrayList<>(participantIds);
        if (!affectedUserIds.contains(payer.getId())) {
            affectedUserIds.add(payer.getId());
        }
        eventPublisher.publishEvent(new GroupExpenseChangedEvent(saved, "CREATED", affectedUserIds));

        return SharedExpenseDto.fromEntity(saved, false);
    }

    @Transactional(readOnly = true)
    public Page<SharedExpenseDto> getGroupExpenses(Long userId, Long groupId, LocalDate from, LocalDate to, Long paidBy, String q, int page, int size) {
        groupService.verifyActiveMember(groupId, userId);

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<SharedExpense> expensePage = sharedExpenseRepository.searchActiveGroupExpenses(groupId, from, to, paidBy, q, pageable);

        return expensePage.map(exp -> {
            boolean locked = isExpenseLocked(exp);
            return SharedExpenseDto.fromEntity(exp, locked);
        });
    }

    @Transactional(readOnly = true)
    public SharedExpenseDto getExpense(Long userId, Long groupId, Long expenseId) {
        groupService.verifyActiveMember(groupId, userId);

        SharedExpense expense = sharedExpenseRepository.findById(expenseId)
                .orElseThrow(() -> ApiException.notFound("Expense not found"));

        if (!expense.getGroup().getId().equals(groupId)) {
            throw ApiException.notFound("Expense not found in this group");
        }

        boolean locked = isExpenseLocked(expense);
        return SharedExpenseDto.fromEntity(expense, locked);
    }

    @Transactional
    public SharedExpenseDto updateExpense(Long userId, Long groupId, Long expenseId, UpdateSharedExpenseRequest request) {
        groupService.verifyActiveMember(groupId, userId);

        // Pessimistic lock
        ExpenseGroup group = groupRepository.findByIdWithLock(groupId)
                .orElseThrow(() -> ApiException.notFound("Group not found"));

        SharedExpense expense = sharedExpenseRepository.findById(expenseId)
                .orElseThrow(() -> ApiException.notFound("Expense not found"));

        if (!expense.getGroup().getId().equals(groupId) || expense.getDeletedAt() != null) {
            throw ApiException.notFound("Expense not found");
        }

        // Creator or Owner only (BR-035)
        GroupMember callerMember = groupMemberRepository.findActiveMember(groupId, userId).orElseThrow();
        boolean isCreator = expense.getCreatedBy().getId().equals(userId);
        boolean isOwner = callerMember.getRole() == GroupRole.OWNER;
        if (!isCreator && !isOwner) {
            throw ApiException.forbidden("Only the creator or group owner may edit this expense");
        }

        if (request.getVersion() != null && !expense.getVersion().equals(request.getVersion())) {
            throw ApiException.conflict(ErrorCode.CONCURRENT_MODIFICATION,
                    "Expense was modified by another session. Please refresh and retry.");
        }

        boolean locked = isExpenseLocked(expense);

        // Check if financial fields are modified when locked (BR-036)
        if (locked) {
            BigDecimal reqAmount = MoneyUtils.scale(request.getAmount());
            if (reqAmount.compareTo(expense.getAmount()) != 0 ||
                !request.getPaidBy().equals(expense.getPaidBy().getId()) ||
                request.getSplitType() != expense.getSplitType()) {
                throw ApiException.businessRule(ErrorCode.EXPENSE_LOCKED_BY_SETTLEMENT,
                        "This expense is locked by confirmed settlements. Financial fields cannot be modified.");
            }
            // Non-financial fields can still be edited (BR-036)
            expense.setDescription(request.getDescription().trim());
            expense.setNotes(request.getNotes());
            if (request.getCategoryId() != null) {
                expense.setCategory(categoryRepository.findById(request.getCategoryId()).orElse(null));
            }
            SharedExpense saved = sharedExpenseRepository.save(expense);
            return SharedExpenseDto.fromEntity(saved, true);
        }

        // Unlocked: full edit
        BigDecimal amount = MoneyUtils.validateAndScale(request.getAmount());
        User payer = userRepository.findById(request.getPaidBy())
                .orElseThrow(() -> ApiException.businessRule(ErrorCode.MEMBER_NOT_ACTIVE, "Payer not found"));

        List<GroupMember> activeMembers = groupMemberRepository.findActiveMembers(groupId);
        Set<Long> activeMemberIds = activeMembers.stream().map(m -> m.getUser().getId()).collect(Collectors.toSet());
        if (!activeMemberIds.contains(payer.getId())) {
            throw ApiException.businessRule(ErrorCode.MEMBER_NOT_ACTIVE, "Payer is not an active member");
        }

        List<SplitItemInput> inputs = new ArrayList<>();
        Set<Long> participantIds = new HashSet<>();
        for (SplitItemRequest splitReq : request.getSplits()) {
            if (!activeMemberIds.contains(splitReq.getUserId())) {
                throw ApiException.businessRule(ErrorCode.MEMBER_NOT_ACTIVE, "Participant is not an active member: " + splitReq.getUserId());
            }
            participantIds.add(splitReq.getUserId());
            inputs.add(new SplitItemInput(splitReq.getUserId(), splitReq.getAmount(), splitReq.getPercentage()));
        }

        List<CalculatedSplitResult> splitResults = splitEngine.computeSplits(request.getSplitType(), amount, inputs);

        expense.setAmount(amount);
        expense.setDescription(request.getDescription().trim());
        expense.setNotes(request.getNotes());
        expense.setExpenseDate(request.getExpenseDate());
        expense.setPaidBy(payer);
        expense.setSplitType(request.getSplitType());
        expense.setUpdatedBy(userRepository.findById(userId).orElse(null));
        if (request.getCategoryId() != null) {
            expense.setCategory(categoryRepository.findById(request.getCategoryId()).orElse(null));
        }

        expense.clearSplits();
        for (CalculatedSplitResult res : splitResults) {
            User splitUser = userRepository.findById(res.getUserId()).orElseThrow();
            expense.addSplit(new ExpenseSplit(expense, splitUser, res.getShareAmount(), res.getPercentage()));
        }

        SharedExpense saved = sharedExpenseRepository.save(expense);

        // Verify balance invariant
        balanceEngine.computeGroupBalances(groupId);

        List<Long> affected = new ArrayList<>(participantIds);
        affected.add(payer.getId());
        eventPublisher.publishEvent(new GroupExpenseChangedEvent(saved, "UPDATED", affected));

        return SharedExpenseDto.fromEntity(saved, false);
    }

    @Transactional
    public void deleteExpense(Long userId, Long groupId, Long expenseId) {
        groupService.verifyActiveMember(groupId, userId);

        ExpenseGroup group = groupRepository.findByIdWithLock(groupId)
                .orElseThrow(() -> ApiException.notFound("Group not found"));

        SharedExpense expense = sharedExpenseRepository.findById(expenseId)
                .orElseThrow(() -> ApiException.notFound("Expense not found"));

        if (!expense.getGroup().getId().equals(groupId) || expense.getDeletedAt() != null) {
            throw ApiException.notFound("Expense not found");
        }

        GroupMember callerMember = groupMemberRepository.findActiveMember(groupId, userId).orElseThrow();
        boolean isCreator = expense.getCreatedBy().getId().equals(userId);
        boolean isOwner = callerMember.getRole() == GroupRole.OWNER;
        if (!isCreator && !isOwner) {
            throw ApiException.forbidden("Only the creator or group owner may delete this expense");
        }

        if (isExpenseLocked(expense)) {
            throw ApiException.businessRule(ErrorCode.EXPENSE_LOCKED_BY_SETTLEMENT,
                    "This expense is locked by confirmed settlements and cannot be deleted");
        }

        // Soft delete (BR-038)
        expense.setDeletedAt(LocalDateTime.now());
        expense.setDeletedBy(userRepository.findById(userId).orElse(null));
        sharedExpenseRepository.save(expense);

        // Verify balance invariant
        balanceEngine.computeGroupBalances(groupId);

        List<Long> affected = expense.getSplits().stream().map(s -> s.getUser().getId()).collect(Collectors.toList());
        affected.add(expense.getPaidBy().getId());
        eventPublisher.publishEvent(new GroupExpenseChangedEvent(expense, "DELETED", affected));
    }

    public boolean isExpenseLocked(SharedExpense expense) {
        // BR-034: locked when at least one CONFIRMED settlement exists with confirmedAt > expense.createdAt
        return settlementRepository.existsConfirmedAfter(expense.getGroup().getId(), expense.getCreatedAt());
    }
}
