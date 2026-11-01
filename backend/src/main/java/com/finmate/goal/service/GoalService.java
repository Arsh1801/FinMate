package com.finmate.goal.service;

import com.finmate.common.exception.ApiException;
import com.finmate.common.exception.ErrorCode;
import com.finmate.common.util.DateUtils;
import com.finmate.common.util.MoneyUtils;
import com.finmate.goal.dto.*;
import com.finmate.goal.entity.GoalContribution;
import com.finmate.goal.entity.GoalStatus;
import com.finmate.goal.entity.SavingsGoal;
import com.finmate.goal.repository.GoalContributionRepository;
import com.finmate.goal.repository.SavingsGoalRepository;
import com.finmate.notification.entity.NotificationType;
import com.finmate.notification.service.NotificationService;
import com.finmate.transaction.entity.TransactionType;
import com.finmate.transaction.repository.TransactionRepository;
import com.finmate.user.entity.User;
import com.finmate.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GoalService {

    private final SavingsGoalRepository goalRepository;
    private final GoalContributionRepository contributionRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final GoalCalculationEngine calculationEngine;
    private final NotificationService notificationService;

    public GoalService(SavingsGoalRepository goalRepository,
                       GoalContributionRepository contributionRepository,
                       UserRepository userRepository,
                       TransactionRepository transactionRepository,
                       GoalCalculationEngine calculationEngine,
                       NotificationService notificationService) {
        this.goalRepository = goalRepository;
        this.contributionRepository = contributionRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.calculationEngine = calculationEngine;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<SavingsGoalDto> getGoals(Long userId, GoalStatus status) {
        List<SavingsGoal> goals = status != null ?
                goalRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status) :
                goalRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return goals.stream().map(goal -> {
            BigDecimal saved = contributionRepository.sumAmountByGoalId(goal.getId());
            return calculationEngine.calculateGoalMetrics(goal, saved);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SavingsGoalDto> getActiveGoals(Long userId) {
        return getGoals(userId, GoalStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public SavingsGoalDto getGoal(Long userId, Long goalId) {
        SavingsGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> ApiException.notFound("Goal not found"));

        if (!goal.getUser().getId().equals(userId)) {
            throw ApiException.notFound("Goal not found");
        }

        BigDecimal saved = contributionRepository.sumAmountByGoalId(goal.getId());
        return calculationEngine.calculateGoalMetrics(goal, saved);
    }

    @Transactional
    public SavingsGoalDto createGoal(Long userId, CreateGoalRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));

        LocalDate today = DateUtils.today();
        if (!request.getTargetDate().isAfter(today)) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Target date must be after today");
        }
        if (request.getTargetDate().isAfter(today.plusYears(10))) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Target date cannot be more than 10 years ahead");
        }

        String name = request.getName().trim();
        goalRepository.findActiveByName(userId, name).ifPresent(g -> {
            throw ApiException.conflict(ErrorCode.VALIDATION_FAILED, "A non-archived goal with name '" + name + "' already exists");
        });

        BigDecimal target = MoneyUtils.validateAndScale(request.getTargetAmount());

        SavingsGoal goal = new SavingsGoal();
        goal.setUser(user);
        goal.setName(name);
        goal.setTargetAmount(target);
        goal.setTargetDate(request.getTargetDate());
        goal.setStatus(GoalStatus.ACTIVE);
        goal.setNote(request.getNote());
        goal.setIcon(request.getIcon());
        goal.setCreatedDate(today);

        SavingsGoal saved = goalRepository.save(goal);
        return calculationEngine.calculateGoalMetrics(saved, BigDecimal.ZERO);
    }

    @Transactional
    public SavingsGoalDto updateGoal(Long userId, Long goalId, UpdateGoalRequest request) {
        SavingsGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> ApiException.notFound("Goal not found"));

        if (!goal.getUser().getId().equals(userId)) {
            throw ApiException.notFound("Goal not found");
        }

        if (goal.getStatus() != GoalStatus.ACTIVE) {
            throw ApiException.businessRule(ErrorCode.GOAL_NOT_ACTIVE, "Only ACTIVE goals can be edited");
        }

        if (request.getVersion() != null && !goal.getVersion().equals(request.getVersion())) {
            throw ApiException.conflict(ErrorCode.CONCURRENT_MODIFICATION,
                    "Goal has been modified by another request. Please refresh and retry.");
        }

        LocalDate today = DateUtils.today();
        if (!request.getTargetDate().isAfter(today)) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Target date must be after today");
        }

        String name = request.getName().trim();
        goalRepository.findActiveByName(userId, name).ifPresent(g -> {
            if (!g.getId().equals(goalId)) {
                throw ApiException.conflict(ErrorCode.VALIDATION_FAILED, "A non-archived goal with name '" + name + "' already exists");
            }
        });

        BigDecimal target = MoneyUtils.validateAndScale(request.getTargetAmount());
        goal.setName(name);
        goal.setTargetAmount(target);
        goal.setTargetDate(request.getTargetDate());
        goal.setNote(request.getNote());

        BigDecimal saved = contributionRepository.sumAmountByGoalId(goalId);
        // BR-024: Lowering target at or below saved amount completes the goal
        if (target.compareTo(saved) <= 0) {
            goal.setStatus(GoalStatus.COMPLETED);
            goal.setCompletedAt(LocalDateTime.now());
        }

        SavingsGoal updated = goalRepository.save(goal);
        return calculationEngine.calculateGoalMetrics(updated, saved);
    }

    @Transactional
    public SavingsGoalDto setGoalStatus(Long userId, Long goalId, GoalStatus newStatus) {
        SavingsGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> ApiException.notFound("Goal not found"));

        if (!goal.getUser().getId().equals(userId)) {
            throw ApiException.notFound("Goal not found");
        }

        if (newStatus == GoalStatus.COMPLETED && goal.getStatus() == GoalStatus.ACTIVE) {
            goal.setStatus(GoalStatus.COMPLETED);
            goal.setCompletedAt(LocalDateTime.now());
        } else if (newStatus == GoalStatus.ARCHIVED) {
            goal.setStatus(GoalStatus.ARCHIVED);
        } else if (newStatus == GoalStatus.ACTIVE) {
            goal.setStatus(GoalStatus.ACTIVE);
            goal.setCompletedAt(null);
        }

        SavingsGoal saved = goalRepository.save(goal);
        BigDecimal currentSaved = contributionRepository.sumAmountByGoalId(goalId);
        return calculationEngine.calculateGoalMetrics(saved, currentSaved);
    }

    @Transactional
    public ContributionResponseDto addContribution(Long userId, Long goalId, AddContributionRequest request) {
        SavingsGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> ApiException.notFound("Goal not found"));

        if (!goal.getUser().getId().equals(userId)) {
            throw ApiException.notFound("Goal not found");
        }

        if (goal.getStatus() != GoalStatus.ACTIVE) {
            throw ApiException.businessRule(ErrorCode.GOAL_NOT_ACTIVE, "Cannot contribute to a non-active goal");
        }

        LocalDate today = DateUtils.today();
        if (request.getContributionDate().isAfter(today)) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Contribution date cannot be in the future");
        }
        if (request.getContributionDate().isBefore(goal.getCreatedDate())) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Contribution date cannot be before goal creation date");
        }

        BigDecimal amount = MoneyUtils.validateAndScale(request.getAmount());

        // BR-025: Free balance protection check
        BigDecimal freeBalance = calculateFreeBalance(userId);
        if (amount.compareTo(freeBalance) > 0) {
            throw ApiException.businessRule(ErrorCode.SAVINGS_INSUFFICIENT_FREE_BALANCE,
                    "Contribution amount (" + amount + ") exceeds available free balance (" + freeBalance + ")");
        }

        GoalContribution contribution = new GoalContribution();
        contribution.setGoal(goal);
        contribution.setAmount(amount);
        contribution.setContributionDate(request.getContributionDate());
        contribution.setNote(request.getNote());

        GoalContribution savedContrib = contributionRepository.save(contribution);

        BigDecimal newSaved = contributionRepository.sumAmountByGoalId(goalId);
        boolean goalCompleted = false;

        // BR-022: Auto-completion when saved >= target
        if (newSaved.compareTo(goal.getTargetAmount()) >= 0) {
            goal.setStatus(GoalStatus.COMPLETED);
            goal.setCompletedAt(LocalDateTime.now());
            goalRepository.save(goal);
            goalCompleted = true;

            // Trigger notification
            notificationService.createNotification(
                    userId,
                    NotificationType.GOAL_COMPLETED,
                    "Goal Completed: " + goal.getName(),
                    "Congratulations! You reached your savings target of " +
                            MoneyUtils.formatIndianCurrency(goal.getTargetAmount()) + " for '" + goal.getName() + "'!",
                    "GOAL",
                    goal.getId(),
                    "goal_completed_" + goal.getId()
            );
        }

        SavingsGoalDto goalDto = calculationEngine.calculateGoalMetrics(goal, newSaved);
        return new ContributionResponseDto(ContributionDto.fromEntity(savedContrib), goalDto, goalCompleted);
    }

    @Transactional
    public void deleteContribution(Long userId, Long goalId, Long contributionId) {
        SavingsGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> ApiException.notFound("Goal not found"));

        if (!goal.getUser().getId().equals(userId)) {
            throw ApiException.notFound("Goal not found");
        }

        if (goal.getStatus() != GoalStatus.ACTIVE) {
            throw ApiException.businessRule(ErrorCode.GOAL_NOT_ACTIVE, "Cannot delete contribution of a non-active goal");
        }

        GoalContribution contribution = contributionRepository.findById(contributionId)
                .orElseThrow(() -> ApiException.notFound("Contribution not found"));

        if (!contribution.getGoal().getId().equals(goalId)) {
            throw ApiException.notFound("Contribution not found");
        }

        contributionRepository.delete(contribution);
    }

    @Transactional(readOnly = true)
    public Page<ContributionDto> getContributions(Long userId, Long goalId, int page, int size) {
        SavingsGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> ApiException.notFound("Goal not found"));

        if (!goal.getUser().getId().equals(userId)) {
            throw ApiException.notFound("Goal not found");
        }

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return contributionRepository.findByGoalIdOrderByContributionDateDescCreatedAtDesc(goalId, pageable)
                .map(ContributionDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateFreeBalance(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));

        LocalDate today = DateUtils.today();
        BigDecimal income = transactionRepository.sumAmountByUserAndTypeDatedBeforeOrOn(userId, TransactionType.INCOME, today);
        BigDecimal expense = transactionRepository.sumAmountByUserAndTypeDatedBeforeOrOn(userId, TransactionType.EXPENSE, today);

        BigDecimal currentBalance = user.getOpeningBalance().add(income).subtract(expense);
        BigDecimal totalSavings = contributionRepository.sumTotalSavingsForUser(userId);

        return currentBalance.subtract(totalSavings);
    }
}
