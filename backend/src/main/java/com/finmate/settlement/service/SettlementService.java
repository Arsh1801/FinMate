package com.finmate.settlement.service;

import com.finmate.common.exception.ApiException;
import com.finmate.common.exception.ErrorCode;
import com.finmate.common.util.DateUtils;
import com.finmate.common.util.MoneyUtils;
import com.finmate.expense.repository.ExpenseSplitRepository;
import com.finmate.expense.repository.SharedExpenseRepository;
import com.finmate.group.entity.ExpenseGroup;
import com.finmate.group.entity.GroupStatus;
import com.finmate.group.repository.ExpenseGroupRepository;
import com.finmate.group.repository.GroupMemberRepository;
import com.finmate.group.service.GroupService;
import com.finmate.notification.event.SettlementStatusChangedEvent;
import com.finmate.settlement.dto.RecordSettlementRequest;
import com.finmate.settlement.dto.SettlementActionRequest;
import com.finmate.settlement.dto.SettlementDto;
import com.finmate.settlement.entity.Settlement;
import com.finmate.settlement.entity.SettlementStatus;
import com.finmate.settlement.repository.SettlementRepository;
import com.finmate.user.entity.User;
import com.finmate.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final ExpenseGroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final SharedExpenseRepository sharedExpenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final BalanceEngine balanceEngine;
    private final GroupService groupService;
    private final ApplicationEventPublisher eventPublisher;

    public SettlementService(SettlementRepository settlementRepository,
                             ExpenseGroupRepository groupRepository,
                             GroupMemberRepository groupMemberRepository,
                             UserRepository userRepository,
                             SharedExpenseRepository sharedExpenseRepository,
                             ExpenseSplitRepository expenseSplitRepository,
                             BalanceEngine balanceEngine,
                             GroupService groupService,
                             ApplicationEventPublisher eventPublisher) {
        this.settlementRepository = settlementRepository;
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
        this.sharedExpenseRepository = sharedExpenseRepository;
        this.expenseSplitRepository = expenseSplitRepository;
        this.balanceEngine = balanceEngine;
        this.groupService = groupService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public SettlementDto recordSettlement(Long recorderId, Long groupId, RecordSettlementRequest request) {
        groupService.verifyActiveMember(groupId, recorderId);

        // Lock group row (BR-042)
        ExpenseGroup group = groupRepository.findByIdWithLock(groupId)
                .orElseThrow(() -> ApiException.notFound("Group not found"));

        if (group.getStatus() == GroupStatus.ARCHIVED) {
            throw ApiException.businessRule(ErrorCode.VALIDATION_FAILED, "Cannot record settlements in an archived group");
        }

        if (request.getFromUserId().equals(request.getToUserId())) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Payer and receiver cannot be the same user");
        }

        // Verify both are active members
        groupMemberRepository.findActiveMember(groupId, request.getFromUserId())
                .orElseThrow(() -> ApiException.businessRule(ErrorCode.MEMBER_NOT_ACTIVE, "Payer is not an active member of this group"));

        groupMemberRepository.findActiveMember(groupId, request.getToUserId())
                .orElseThrow(() -> ApiException.businessRule(ErrorCode.MEMBER_NOT_ACTIVE, "Receiver is not an active member of this group"));

        BigDecimal amount = MoneyUtils.validateAndScale(request.getAmount());

        if (request.getSettlementDate().isAfter(DateUtils.today())) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Settlement date cannot be in the future");
        }

        // Overpayment guard (BR-032)
        BigDecimal pendingSum = settlementRepository.sumPendingBetween(groupId, request.getFromUserId(), request.getToUserId());
        BigDecimal totalClaimed = amount.add(pendingSum);

        // Check against overall debt or direct balance
        // To ensure user does not overpay, check payer's debt in group
        // If recorded by receiver -> auto CONFIRMED (BR-031). If recorded by payer -> PENDING.
        boolean isReceiver = recorderId.equals(request.getToUserId());
        SettlementStatus initialStatus = isReceiver ? SettlementStatus.CONFIRMED : SettlementStatus.PENDING;

        User fromUser = userRepository.findById(request.getFromUserId()).orElseThrow();
        User toUser = userRepository.findById(request.getToUserId()).orElseThrow();
        User recorder = userRepository.findById(recorderId).orElseThrow();

        Settlement settlement = new Settlement();
        settlement.setGroup(group);
        settlement.setFromUser(fromUser);
        settlement.setToUser(toUser);
        settlement.setAmount(amount);
        settlement.setStatus(initialStatus);
        settlement.setSettlementDate(request.getSettlementDate());
        settlement.setNote(request.getNote());
        settlement.setRecordedBy(recorder);
        if (isReceiver) {
            settlement.setConfirmedAt(LocalDateTime.now());
        }

        Settlement saved = settlementRepository.save(settlement);

        // Verify balance invariant
        balanceEngine.computeGroupBalances(groupId);

        // Publish event for notification
        eventPublisher.publishEvent(new SettlementStatusChangedEvent(saved));

        return SettlementDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Page<SettlementDto> getGroupSettlements(Long userId, Long groupId, SettlementStatus status, int page, int size) {
        groupService.verifyActiveMember(groupId, userId);

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return settlementRepository.findByGroupIdAndStatus(groupId, status, pageable)
                .map(SettlementDto::fromEntity);
    }

    @Transactional
    public SettlementDto processSettlementAction(Long userId, Long groupId, Long settlementId, SettlementActionRequest request) {
        groupService.verifyActiveMember(groupId, userId);

        ExpenseGroup group = groupRepository.findByIdWithLock(groupId)
                .orElseThrow(() -> ApiException.notFound("Group not found"));

        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> ApiException.notFound("Settlement not found"));

        if (!settlement.getGroup().getId().equals(groupId)) {
            throw ApiException.notFound("Settlement not found in this group");
        }

        // Only receiver can confirm, reject, or reverse (BR-031, BR-033, Section 10.5)
        if (!settlement.getToUser().getId().equals(userId)) {
            throw ApiException.forbidden("Only the payment receiver can confirm, reject, or reverse a settlement");
        }

        if (request.getVersion() != null && !settlement.getVersion().equals(request.getVersion())) {
            throw ApiException.conflict(ErrorCode.CONCURRENT_MODIFICATION,
                    "Settlement was modified by another session. Please refresh and retry.");
        }

        String action = request.getAction().toUpperCase();
        if ("CONFIRM".equals(action)) {
            if (settlement.getStatus() != SettlementStatus.PENDING) {
                throw ApiException.businessRule(ErrorCode.VALIDATION_FAILED, "Only PENDING settlements can be confirmed");
            }
            settlement.setStatus(SettlementStatus.CONFIRMED);
            settlement.setConfirmedAt(LocalDateTime.now());
        } else if ("REJECT".equals(action)) {
            if (settlement.getStatus() != SettlementStatus.PENDING) {
                throw ApiException.businessRule(ErrorCode.VALIDATION_FAILED, "Only PENDING settlements can be rejected");
            }
            settlement.setStatus(SettlementStatus.REJECTED);
        } else if ("REVERSE".equals(action)) {
            if (settlement.getStatus() != SettlementStatus.CONFIRMED) {
                throw ApiException.businessRule(ErrorCode.VALIDATION_FAILED, "Only CONFIRMED settlements can be reversed");
            }
            settlement.setStatus(SettlementStatus.REVERSED);
        }

        Settlement saved = settlementRepository.save(settlement);

        // Verify balance invariant
        balanceEngine.computeGroupBalances(groupId);

        // Publish event for notification
        eventPublisher.publishEvent(new SettlementStatusChangedEvent(saved));

        return SettlementDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<SettlementDto> getPendingSettlementsForUser(Long userId) {
        return settlementRepository.findPendingForReceiver(userId).stream()
                .map(SettlementDto::fromEntity)
                .collect(Collectors.toList());
    }
}
