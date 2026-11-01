package com.finmate.budget.service;

import com.finmate.budget.dto.*;
import com.finmate.budget.entity.Budget;
import com.finmate.budget.entity.BudgetAlertLevel;
import com.finmate.budget.entity.BudgetStatus;
import com.finmate.budget.repository.BudgetRepository;
import com.finmate.category.entity.Category;
import com.finmate.category.entity.CategoryType;
import com.finmate.category.service.CategoryService;
import com.finmate.common.exception.ApiException;
import com.finmate.common.exception.ErrorCode;
import com.finmate.common.util.DateUtils;
import com.finmate.common.util.MoneyUtils;
import com.finmate.notification.service.NotificationService;
import com.finmate.transaction.repository.TransactionRepository;
import com.finmate.user.entity.User;
import com.finmate.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final CategoryService categoryService;
    private final TransactionRepository transactionRepository;
    private final BudgetCalculationEngine calculationEngine;
    private final NotificationService notificationService;

    public BudgetService(BudgetRepository budgetRepository,
                         UserRepository userRepository,
                         CategoryService categoryService,
                         TransactionRepository transactionRepository,
                         BudgetCalculationEngine calculationEngine,
                         NotificationService notificationService) {
        this.budgetRepository = budgetRepository;
        this.userRepository = userRepository;
        this.categoryService = categoryService;
        this.transactionRepository = transactionRepository;
        this.calculationEngine = calculationEngine;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<BudgetDto> getBudgetsForMonth(Long userId, Integer year, Integer month) {
        LocalDate now = DateUtils.today();
        int targetYear = year != null ? year : now.getYear();
        int targetMonth = month != null ? month : now.getMonthValue();

        List<Budget> budgets = budgetRepository.findByUserIdAndBudgetYearAndBudgetMonth(userId, targetYear, targetMonth);
        YearMonth ym = YearMonth.of(targetYear, targetMonth);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        return budgets.stream().map(budget -> {
            BigDecimal spent = transactionRepository.sumExpenseInPeriod(
                    userId,
                    budget.getCategory() != null ? budget.getCategory().getId() : null,
                    start,
                    end
            );
            return calculationEngine.calculateBudgetMetrics(budget, spent);
        }).collect(Collectors.toList());
    }

    @Transactional
    public BudgetDto createBudget(Long userId, CreateBudgetRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));

        validateBudgetMonth(request.getYear(), request.getMonth(), false);
        BigDecimal limit = MoneyUtils.validateAndScale(request.getLimitAmount());

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryService.getCategoryForUserAndType(request.getCategoryId(), userId, CategoryType.EXPENSE);
            if (budgetRepository.findByUserIdAndBudgetYearAndBudgetMonthAndCategory(userId, request.getYear(), request.getMonth(), category).isPresent()) {
                throw ApiException.conflict(ErrorCode.BUDGET_EXISTS, "A budget for this category and month already exists");
            }
        } else {
            if (budgetRepository.findByUserIdAndBudgetYearAndBudgetMonthAndCategoryIsNull(userId, request.getYear(), request.getMonth()).isPresent()) {
                throw ApiException.conflict(ErrorCode.BUDGET_EXISTS, "An overall budget for this month already exists");
            }
        }

        // BR-055: category budgets sum <= overall budget
        validateOverallSumInvariant(userId, request.getYear(), request.getMonth(), category, limit, null);

        Budget budget = new Budget();
        budget.setUser(user);
        budget.setCategory(category);
        budget.setBudgetYear(request.getYear());
        budget.setBudgetMonth(request.getMonth());
        budget.setLimitAmount(limit);
        budget.setAlertThreshold(request.getAlertThreshold() != null ? request.getAlertThreshold() : user.getDefaultAlertThreshold());
        budget.setLastAlertLevel(BudgetAlertLevel.NONE);

        Budget saved = budgetRepository.save(budget);

        YearMonth ym = YearMonth.of(request.getYear(), request.getMonth());
        BigDecimal spent = transactionRepository.sumExpenseInPeriod(
                userId,
                saved.getCategory() != null ? saved.getCategory().getId() : null,
                ym.atDay(1),
                ym.atEndOfMonth()
        );

        return calculationEngine.calculateBudgetMetrics(saved, spent);
    }

    @Transactional
    public BudgetDto updateBudget(Long userId, Long budgetId, UpdateBudgetRequest request) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> ApiException.notFound("Budget not found"));

        if (!budget.getUser().getId().equals(userId)) {
            throw ApiException.notFound("Budget not found");
        }

        // Check optimistic lock
        if (request.getVersion() != null && !budget.getVersion().equals(request.getVersion())) {
            throw ApiException.conflict(ErrorCode.CONCURRENT_MODIFICATION,
                    "Budget has been modified by another request. Please refresh and retry.");
        }

        validateBudgetMonth(budget.getBudgetYear(), budget.getBudgetMonth(), true);
        BigDecimal newLimit = MoneyUtils.validateAndScale(request.getLimitAmount());

        // Validate overall sum invariant
        validateOverallSumInvariant(userId, budget.getBudgetYear(), budget.getBudgetMonth(), budget.getCategory(), newLimit, budget.getId());

        budget.setLimitAmount(newLimit);
        if (request.getAlertThreshold() != null) {
            budget.setAlertThreshold(request.getAlertThreshold());
        }

        Budget saved = budgetRepository.save(budget);

        YearMonth ym = YearMonth.of(saved.getBudgetYear(), saved.getBudgetMonth());
        BigDecimal spent = transactionRepository.sumExpenseInPeriod(
                userId,
                saved.getCategory() != null ? saved.getCategory().getId() : null,
                ym.atDay(1),
                ym.atEndOfMonth()
        );

        return calculationEngine.calculateBudgetMetrics(saved, spent);
    }

    @Transactional
    public void deleteBudget(Long userId, Long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> ApiException.notFound("Budget not found"));

        if (!budget.getUser().getId().equals(userId)) {
            throw ApiException.notFound("Budget not found");
        }

        validateBudgetMonth(budget.getBudgetYear(), budget.getBudgetMonth(), true);
        budgetRepository.delete(budget);
    }

    @Transactional
    public Map<String, Integer> copyBudgets(Long userId, CopyBudgetsRequest request) {
        validateBudgetMonth(request.getToYear(), request.getToMonth(), true);

        List<Budget> sourceBudgets = budgetRepository.findByUserIdAndBudgetYearAndBudgetMonth(
                userId, request.getFromYear(), request.getFromMonth());

        int created = 0;
        int skipped = 0;

        for (Budget src : sourceBudgets) {
            boolean exists;
            if (src.getCategory() != null) {
                exists = budgetRepository.findByUserIdAndBudgetYearAndBudgetMonthAndCategory(
                        userId, request.getToYear(), request.getToMonth(), src.getCategory()).isPresent();
            } else {
                exists = budgetRepository.findByUserIdAndBudgetYearAndBudgetMonthAndCategoryIsNull(
                        userId, request.getToYear(), request.getToMonth()).isPresent();
            }

            if (exists) {
                skipped++;
            } else {
                Budget copy = new Budget();
                copy.setUser(src.getUser());
                copy.setCategory(src.getCategory());
                copy.setBudgetYear(request.getToYear());
                copy.setBudgetMonth(request.getToMonth());
                copy.setLimitAmount(src.getLimitAmount());
                copy.setAlertThreshold(src.getAlertThreshold());
                copy.setLastAlertLevel(BudgetAlertLevel.NONE);
                budgetRepository.save(copy);
                created++;
            }
        }

        return Map.of("created", created, "skipped", skipped);
    }

    @Transactional(readOnly = true)
    public List<BudgetHistoryDto> getBudgetHistory(Long userId, Integer months, Long categoryId) {
        int limitMonths = (months != null && months >= 1 && months <= 36) ? months : 12;
        LocalDate now = DateUtils.today();
        LocalDate startLimit = now.minusMonths(limitMonths);

        List<Budget> allBudgets = budgetRepository.findBudgetHistory(userId, categoryId);
        List<BudgetHistoryDto> history = new ArrayList<>();

        for (Budget b : allBudgets) {
            YearMonth ym = YearMonth.of(b.getBudgetYear(), b.getBudgetMonth());
            if (ym.atDay(1).isBefore(startLimit)) continue;

            BigDecimal spent = transactionRepository.sumExpenseInPeriod(
                    userId,
                    b.getCategory() != null ? b.getCategory().getId() : null,
                    ym.atDay(1),
                    ym.atEndOfMonth()
            );

            BudgetDto metrics = calculationEngine.calculateBudgetMetrics(b, spent);
            history.add(new BudgetHistoryDto(
                    b.getBudgetYear(),
                    b.getBudgetMonth(),
                    metrics.getCategory(),
                    metrics.getLimitAmount(),
                    metrics.getSpentAmount(),
                    metrics.getRemainingAmount(),
                    metrics.getUtilizationPercent(),
                    metrics.getStatus()
            ));
        }

        return history;
    }

    @Transactional
    public void evaluateBudgetAlerts(Long userId, int year, int month) {
        List<Budget> budgets = budgetRepository.findByUserIdAndBudgetYearAndBudgetMonth(userId, year, month);
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        for (Budget budget : budgets) {
            BigDecimal spent = transactionRepository.sumExpenseInPeriod(
                    userId,
                    budget.getCategory() != null ? budget.getCategory().getId() : null,
                    start,
                    end
            );
            BudgetDto metrics = calculationEngine.calculateBudgetMetrics(budget, spent);
            BudgetAlertLevel currentLevel = switch (metrics.getStatus()) {
                case SAFE -> BudgetAlertLevel.NONE;
                case WARNING -> BudgetAlertLevel.WARNING;
                case EXCEEDED -> BudgetAlertLevel.EXCEEDED;
            };

            BudgetAlertLevel previousLevel = budget.getLastAlertLevel();

            // BR-056: Alerts fire on level change upward only; lowered when recalculation shows a lower level
            if (currentLevel.ordinal() > previousLevel.ordinal()) {
                budget.setLastAlertLevel(currentLevel);
                budgetRepository.save(budget);

                String categoryName = budget.getCategory() != null ? budget.getCategory().getName() : "Overall";
                String dedupeKey = "budget_" + budget.getId() + "_" + year + "_" + month + "_" + currentLevel.name();

                if (currentLevel == BudgetAlertLevel.WARNING) {
                    notificationService.createNotification(
                            userId,
                            com.finmate.notification.entity.NotificationType.BUDGET_WARNING,
                            "Budget Warning: " + categoryName,
                            String.format("%s budget has reached %.1f%% utilization (Spent %s of %s)",
                                    categoryName, metrics.getUtilizationPercent(),
                                    MoneyUtils.formatIndianCurrency(new BigDecimal(metrics.getSpentAmount())),
                                    MoneyUtils.formatIndianCurrency(new BigDecimal(metrics.getLimitAmount()))),
                            "BUDGET",
                            budget.getId(),
                            dedupeKey
                    );
                } else if (currentLevel == BudgetAlertLevel.EXCEEDED) {
                    notificationService.createNotification(
                            userId,
                            com.finmate.notification.entity.NotificationType.BUDGET_EXCEEDED,
                            "Budget Exceeded: " + categoryName,
                            String.format("%s budget exceeded! Overspent by %s (Total spent %s against limit %s)",
                                    categoryName,
                                    MoneyUtils.formatIndianCurrency(new BigDecimal(metrics.getOverspentAmount())),
                                    MoneyUtils.formatIndianCurrency(new BigDecimal(metrics.getSpentAmount())),
                                    MoneyUtils.formatIndianCurrency(new BigDecimal(metrics.getLimitAmount()))),
                            "BUDGET",
                            budget.getId(),
                            dedupeKey
                    );
                }
            } else if (currentLevel.ordinal() < previousLevel.ordinal()) {
                // Lower lastAlertLevel without sending notification
                budget.setLastAlertLevel(currentLevel);
                budgetRepository.save(budget);
            }
        }
    }

    private void validateBudgetMonth(int year, int month, boolean isEditOrDelete) {
        LocalDate today = DateUtils.today();
        YearMonth currentYm = YearMonth.from(today);
        YearMonth targetYm = YearMonth.of(year, month);

        if (targetYm.isBefore(currentYm)) {
            throw ApiException.businessRule(ErrorCode.BUDGET_PERIOD_LOCKED, "Past-month budgets are locked and read-only");
        }

        if (!isEditOrDelete) {
            YearMonth maxAhead = currentYm.plusMonths(3);
            if (targetYm.isAfter(maxAhead)) {
                throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Budget cannot be created more than 3 months ahead");
            }
        }
    }

    private void validateOverallSumInvariant(Long userId, int year, int month, Category category, BigDecimal newLimit, Long currentBudgetId) {
        Optional<Budget> overallOpt = budgetRepository.findByUserIdAndBudgetYearAndBudgetMonthAndCategoryIsNull(userId, year, month);
        List<Budget> categoryBudgets = budgetRepository.findCategoryBudgetsForMonth(userId, year, month);

        if (category == null) {
            // Updating or creating overall budget
            BigDecimal catSum = BigDecimal.ZERO;
            for (Budget b : categoryBudgets) {
                catSum = catSum.add(b.getLimitAmount());
            }
            if (newLimit.compareTo(catSum) < 0) {
                throw ApiException.businessRule(ErrorCode.BUDGET_SUM_EXCEEDS_OVERALL,
                        "Overall budget limit (" + newLimit + ") cannot be less than the sum of category budgets (" + catSum + ")");
            }
        } else if (overallOpt.isPresent()) {
            // Updating or creating category budget
            BigDecimal overallLimit = overallOpt.get().getLimitAmount();
            BigDecimal catSum = BigDecimal.ZERO;
            for (Budget b : categoryBudgets) {
                if (currentBudgetId == null || !b.getId().equals(currentBudgetId)) {
                    catSum = catSum.add(b.getLimitAmount());
                }
            }
            catSum = catSum.add(newLimit);
            if (catSum.compareTo(overallLimit) > 0) {
                throw ApiException.businessRule(ErrorCode.BUDGET_SUM_EXCEEDS_OVERALL,
                        "Total category budgets (" + catSum + ") would exceed overall budget (" + overallLimit + ")");
            }
        }
    }
}
