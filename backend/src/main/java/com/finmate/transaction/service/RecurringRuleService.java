package com.finmate.transaction.service;

import com.finmate.category.entity.Category;
import com.finmate.category.entity.CategoryType;
import com.finmate.category.service.CategoryService;
import com.finmate.common.exception.ApiException;
import com.finmate.common.exception.ErrorCode;
import com.finmate.common.util.DateUtils;
import com.finmate.common.util.MoneyUtils;
import com.finmate.transaction.dto.CreateRecurringRuleRequest;
import com.finmate.transaction.dto.CreateTransactionRequest;
import com.finmate.transaction.dto.RecurringRuleDto;
import com.finmate.transaction.entity.RecurrenceFrequency;
import com.finmate.transaction.entity.RecurringRule;
import com.finmate.transaction.entity.Transaction;
import com.finmate.transaction.repository.RecurringRuleRepository;
import com.finmate.transaction.repository.TransactionRepository;
import com.finmate.user.entity.User;
import com.finmate.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecurringRuleService {

    private static final Logger log = LoggerFactory.getLogger(RecurringRuleService.class);

    private final RecurringRuleRepository recurringRuleRepository;
    private final UserRepository userRepository;
    private final CategoryService categoryService;
    private final TransactionRepository transactionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public RecurringRuleService(RecurringRuleRepository recurringRuleRepository,
                                UserRepository userRepository,
                                CategoryService categoryService,
                                TransactionRepository transactionRepository,
                                ApplicationEventPublisher eventPublisher) {
        this.recurringRuleRepository = recurringRuleRepository;
        this.userRepository = userRepository;
        this.categoryService = categoryService;
        this.transactionRepository = transactionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<RecurringRuleDto> getRulesForUser(Long userId) {
        return recurringRuleRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(RecurringRuleDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public RecurringRuleDto createRule(Long userId, CreateRecurringRuleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));

        if (request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate())) {
            throw ApiException.businessRule(ErrorCode.VALIDATION_FAILED, "End date cannot be before start date");
        }

        BigDecimal amount = MoneyUtils.validateAndScale(request.getAmount());

        CategoryType expectedType = request.getType() == com.finmate.transaction.entity.TransactionType.INCOME ?
                CategoryType.INCOME : CategoryType.EXPENSE;
        Category category = categoryService.getCategoryForUserAndType(request.getCategoryId(), userId, expectedType);

        RecurringRule rule = new RecurringRule();
        rule.setUser(user);
        rule.setCategory(category);
        rule.setType(request.getType());
        rule.setAmount(amount);
        rule.setDescription(request.getDescription().trim());
        rule.setPaymentMethod(request.getPaymentMethod());
        rule.setFrequency(request.getFrequency());
        rule.setStartDate(request.getStartDate());
        rule.setEndDate(request.getEndDate());
        rule.setNextRunDate(request.getStartDate());
        rule.setIsActive(true);

        RecurringRule saved = recurringRuleRepository.save(rule);
        return RecurringRuleDto.fromEntity(saved);
    }

    @Transactional
    public RecurringRuleDto updateRule(Long userId, Long ruleId, CreateRecurringRuleRequest request) {
        RecurringRule rule = recurringRuleRepository.findById(ruleId)
                .orElseThrow(() -> ApiException.notFound("Recurring rule not found"));

        if (!rule.getUser().getId().equals(userId)) {
            throw ApiException.notFound("Recurring rule not found");
        }

        if (request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate())) {
            throw ApiException.businessRule(ErrorCode.VALIDATION_FAILED, "End date cannot be before start date");
        }

        BigDecimal amount = MoneyUtils.validateAndScale(request.getAmount());

        CategoryType expectedType = request.getType() == com.finmate.transaction.entity.TransactionType.INCOME ?
                CategoryType.INCOME : CategoryType.EXPENSE;
        Category category = categoryService.getCategoryForUserAndType(request.getCategoryId(), userId, expectedType);

        rule.setAmount(amount);
        rule.setCategory(category);
        rule.setDescription(request.getDescription().trim());
        rule.setPaymentMethod(request.getPaymentMethod());
        rule.setFrequency(request.getFrequency());
        rule.setStartDate(request.getStartDate());
        rule.setEndDate(request.getEndDate());

        RecurringRule saved = recurringRuleRepository.save(rule);
        return RecurringRuleDto.fromEntity(saved);
    }

    @Transactional
    public void deleteRule(Long userId, Long ruleId) {
        RecurringRule rule = recurringRuleRepository.findById(ruleId)
                .orElseThrow(() -> ApiException.notFound("Recurring rule not found"));

        if (!rule.getUser().getId().equals(userId)) {
            throw ApiException.notFound("Recurring rule not found");
        }

        rule.setIsActive(false);
        recurringRuleRepository.delete(rule);
    }

    @Transactional
    public int processDueRules(LocalDate today) {
        List<RecurringRule> dueRules = recurringRuleRepository.findDueRules(today);
        int generatedCount = 0;

        for (RecurringRule rule : dueRules) {
            LocalDate runDate = rule.getNextRunDate();
            int maxCatchUp = 31; // BR-015
            int iterations = 0;

            while (!runDate.isAfter(today) && iterations < maxCatchUp) {
                if (rule.getEndDate() != null && runDate.isAfter(rule.getEndDate())) {
                    rule.setIsActive(false);
                    break;
                }

                // Create recurring transaction
                Transaction transaction = new Transaction();
                transaction.setUser(rule.getUser());
                transaction.setCategory(rule.getCategory());
                transaction.setType(rule.getType());
                transaction.setAmount(rule.getAmount());
                transaction.setTransactionDate(runDate);
                transaction.setDescription(rule.getDescription());
                transaction.setPaymentMethod(rule.getPaymentMethod());
                transaction.setRecurringRule(rule);
                transactionRepository.save(transaction);
                generatedCount++;

                // Advance next run date
                runDate = advanceDate(runDate, rule.getFrequency());
                iterations++;
            }

            rule.setNextRunDate(runDate);
            if (rule.getEndDate() != null && runDate.isAfter(rule.getEndDate())) {
                rule.setIsActive(false);
            }
            recurringRuleRepository.save(rule);
        }

        return generatedCount;
    }

    private LocalDate advanceDate(LocalDate date, RecurrenceFrequency frequency) {
        return switch (frequency) {
            case WEEKLY -> date.plusWeeks(1);
            case MONTHLY -> date.plusMonths(1);
            case YEARLY -> date.plusYears(1);
        };
    }
}
