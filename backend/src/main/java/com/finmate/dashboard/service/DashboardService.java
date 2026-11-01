package com.finmate.dashboard.service;

import com.finmate.budget.dto.BudgetDto;
import com.finmate.budget.service.BudgetService;
import com.finmate.common.util.DateUtils;
import com.finmate.common.util.MoneyUtils;
import com.finmate.dashboard.dto.*;
import com.finmate.goal.dto.SavingsGoalDto;
import com.finmate.goal.service.GoalService;
import com.finmate.group.entity.ExpenseGroup;
import com.finmate.group.repository.ExpenseGroupRepository;
import com.finmate.settlement.dto.GroupBalancesDto;
import com.finmate.settlement.dto.MemberBalanceDto;
import com.finmate.settlement.service.BalanceEngine;
import com.finmate.transaction.entity.Transaction;
import com.finmate.transaction.entity.TransactionType;
import com.finmate.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final BudgetService budgetService;
    private final GoalService goalService;
    private final ExpenseGroupRepository groupRepository;
    private final BalanceEngine balanceEngine;

    public DashboardService(TransactionRepository transactionRepository,
                            BudgetService budgetService,
                            GoalService goalService,
                            ExpenseGroupRepository groupRepository,
                            BalanceEngine balanceEngine) {
        this.transactionRepository = transactionRepository;
        this.budgetService = budgetService;
        this.goalService = goalService;
        this.groupRepository = groupRepository;
        this.balanceEngine = balanceEngine;
    }

    public DashboardSummaryDto getSummary(Long userId, String period) {
        LocalDate today = DateUtils.today();
        LocalDate startDate;
        LocalDate endDate = today;

        LocalDate priorStartDate;
        LocalDate priorEndDate;

        if ("last_month".equalsIgnoreCase(period)) {
            YearMonth lastMonth = YearMonth.from(today).minusMonths(1);
            startDate = lastMonth.atDay(1);
            endDate = lastMonth.atEndOfMonth();

            YearMonth priorMonth = lastMonth.minusMonths(1);
            priorStartDate = priorMonth.atDay(1);
            priorEndDate = priorMonth.atEndOfMonth();
        } else if ("last_3_months".equalsIgnoreCase(period)) {
            startDate = today.minusMonths(3);
            priorStartDate = startDate.minusMonths(3);
            priorEndDate = startDate.minusDays(1);
        } else if ("this_year".equalsIgnoreCase(period)) {
            startDate = LocalDate.of(today.getYear(), 1, 1);
            priorStartDate = LocalDate.of(today.getYear() - 1, 1, 1);
            priorEndDate = LocalDate.of(today.getYear() - 1, today.getMonthValue(), Math.min(today.getDayOfMonth(), 28));
        } else { // default "this_month"
            startDate = today.withDayOfMonth(1);
            int dayOfMonth = today.getDayOfMonth();
            YearMonth prevMonth = YearMonth.from(today).minusMonths(1);
            priorStartDate = prevMonth.atDay(1);
            priorEndDate = prevMonth.atDay(Math.min(dayOfMonth, prevMonth.lengthOfMonth()));
        }

        BigDecimal income = transactionRepository.sumByTypeInPeriod(userId, TransactionType.INCOME, startDate, endDate);
        BigDecimal expense = transactionRepository.sumByTypeInPeriod(userId, TransactionType.EXPENSE, startDate, endDate);
        BigDecimal netSavings = income.subtract(expense);

        BigDecimal priorIncome = transactionRepository.sumByTypeInPeriod(userId, TransactionType.INCOME, priorStartDate, priorEndDate);
        BigDecimal priorExpense = transactionRepository.sumByTypeInPeriod(userId, TransactionType.EXPENSE, priorStartDate, priorEndDate);
        BigDecimal priorNetSavings = priorIncome.subtract(priorExpense);

        // All time cumulative for current balance
        BigDecimal allTimeIncome = transactionRepository.sumAmountByUserAndTypeDatedBeforeOrOn(userId, TransactionType.INCOME, today);
        BigDecimal allTimeExpense = transactionRepository.sumAmountByUserAndTypeDatedBeforeOrOn(userId, TransactionType.EXPENSE, today);
        BigDecimal currentBalance = allTimeIncome.subtract(allTimeExpense);

        // Savings locked in active goals
        List<SavingsGoalDto> activeGoals = goalService.getActiveGoals(userId);
        BigDecimal totalSavings = activeGoals.stream()
                .map(g -> new BigDecimal(g.getSavedAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal freeBalance = currentBalance.subtract(totalSavings);

        // Savings rate
        BigDecimal savingsRate = BigDecimal.ZERO;
        if (income.compareTo(BigDecimal.ZERO) > 0) {
            savingsRate = netSavings.divide(income, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
        }

        // Daily average spend & projected month-end
        int daysPassed = Math.max(1, (int) ChronoUnit.DAYS.between(startDate, endDate) + 1);
        BigDecimal avgDailySpend = expense.divide(BigDecimal.valueOf(daysPassed), 2, RoundingMode.HALF_UP);

        YearMonth ym = YearMonth.from(today);
        int totalDaysInMonth = ym.lengthOfMonth();
        BigDecimal projectedMonthEnd = avgDailySpend.multiply(BigDecimal.valueOf(totalDaysInMonth)).setScale(2, RoundingMode.HALF_UP);

        // Deltas
        Map<String, String> deltas = new HashMap<>();
        deltas.put("income", calculateDelta(income, priorIncome));
        deltas.put("expense", calculateDelta(expense, priorExpense));
        deltas.put("savings", calculateDelta(netSavings, priorNetSavings));

        DashboardSummaryDto dto = new DashboardSummaryDto();
        dto.setTotalIncome(MoneyUtils.formatIndian(income));
        dto.setTotalExpense(MoneyUtils.formatIndian(expense));
        dto.setCurrentBalance(MoneyUtils.formatIndian(currentBalance));
        dto.setTotalSavings(MoneyUtils.formatIndian(totalSavings));
        dto.setFreeBalance(MoneyUtils.formatIndian(freeBalance));
        dto.setNetSavings(MoneyUtils.formatIndian(netSavings));
        dto.setSavingsRate(savingsRate);
        dto.setAvgDailySpend(MoneyUtils.formatIndian(avgDailySpend));
        dto.setProjectedMonthEnd(MoneyUtils.formatIndian(projectedMonthEnd));
        dto.setDeltas(deltas);

        return dto;
    }

    private String calculateDelta(BigDecimal current, BigDecimal prior) {
        if (prior == null || prior.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? "+100%" : "0%";
        }
        BigDecimal diff = current.subtract(prior);
        BigDecimal pct = diff.divide(prior.abs(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP);
        String prefix = pct.compareTo(BigDecimal.ZERO) > 0 ? "+" : "";
        return prefix + pct.toPlainString() + "%";
    }

    public List<MonthlyIncomeExpenseDto> getMonthlyIncomeExpense(Long userId, int months) {
        List<MonthlyIncomeExpenseDto> list = new ArrayList<>();
        YearMonth current = YearMonth.from(DateUtils.today());
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MMM yyyy");

        for (int i = months - 1; i >= 0; i--) {
            YearMonth targetMonth = current.minusMonths(i);
            LocalDate start = targetMonth.atDay(1);
            LocalDate end = targetMonth.atEndOfMonth();

            BigDecimal inc = transactionRepository.sumByTypeInPeriod(userId, TransactionType.INCOME, start, end);
            BigDecimal exp = transactionRepository.sumByTypeInPeriod(userId, TransactionType.EXPENSE, start, end);
            BigDecimal net = inc.subtract(exp);

            list.add(new MonthlyIncomeExpenseDto(
                    targetMonth.format(labelFormatter),
                    inc,
                    exp,
                    net
            ));
        }
        return list;
    }

    public List<CategoryDistributionDto> getCategoryDistribution(Long userId, String monthStr) {
        YearMonth ym = (monthStr != null && !monthStr.isBlank()) ? YearMonth.parse(monthStr) : YearMonth.from(DateUtils.today());
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<Transaction> transactions = transactionRepository.findByUserIdAndTransactionDateBetween(userId, start, end);
        Map<String, BigDecimal> categorySums = new HashMap<>();
        Map<String, String> categoryColors = new HashMap<>();

        BigDecimal totalExpense = BigDecimal.ZERO;
        for (Transaction t : transactions) {
            if (t.getType() == TransactionType.EXPENSE) {
                String catName = t.getCategory() != null ? t.getCategory().getName() : "Uncategorized";
                String color = (t.getCategory() != null && t.getCategory().getColorHex() != null) ? t.getCategory().getColorHex() : "#64748b";

                categorySums.put(catName, categorySums.getOrDefault(catName, BigDecimal.ZERO).add(t.getAmount()));
                categoryColors.put(catName, color);
                totalExpense = totalExpense.add(t.getAmount());
            }
        }

        if (totalExpense.compareTo(BigDecimal.ZERO) == 0) {
            return Collections.emptyList();
        }

        final BigDecimal finalTotal = totalExpense;
        List<Map.Entry<String, BigDecimal>> sorted = categorySums.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toList());

        List<CategoryDistributionDto> result = new ArrayList<>();
        BigDecimal otherAmount = BigDecimal.ZERO;

        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<String, BigDecimal> entry = sorted.get(i);
            if (i < 5) {
                BigDecimal pct = entry.getValue().divide(finalTotal, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
                result.add(new CategoryDistributionDto(
                        entry.getKey(),
                        entry.getValue(),
                        pct,
                        categoryColors.getOrDefault(entry.getKey(), "#64748b")
                ));
            } else {
                otherAmount = otherAmount.add(entry.getValue());
            }
        }

        if (otherAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal pct = otherAmount.divide(finalTotal, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
            result.add(new CategoryDistributionDto("Other", otherAmount, pct, "#94a3b8"));
        }

        return result;
    }

    public List<SpendingTrendDto> getSpendingTrends(Long userId, int days) {
        LocalDate today = DateUtils.today();
        LocalDate start = today.minusDays(days - 1);

        List<Transaction> transactions = transactionRepository.findByUserIdAndTransactionDateBetween(userId, start, today);
        Map<LocalDate, BigDecimal> dailySpend = new HashMap<>();

        for (Transaction t : transactions) {
            if (t.getType() == TransactionType.EXPENSE) {
                dailySpend.put(t.getTransactionDate(), dailySpend.getOrDefault(t.getTransactionDate(), BigDecimal.ZERO).add(t.getAmount()));
            }
        }

        List<SpendingTrendDto> trends = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd");

        for (int i = 0; i < days; i++) {
            LocalDate d = start.plusDays(i);
            BigDecimal amt = dailySpend.getOrDefault(d, BigDecimal.ZERO);
            trends.add(new SpendingTrendDto(d.toString(), d.format(fmt), amt));
        }

        return trends;
    }

    public List<BudgetUtilizationBarDto> getBudgetUtilizationBars(Long userId, Integer year, Integer month) {
        if (year == null || month == null) {
            LocalDate today = DateUtils.today();
            year = today.getYear();
            month = today.getMonthValue();
        }

        List<BudgetDto> budgets = budgetService.getBudgetsForMonth(userId, year, month);
        List<BudgetUtilizationBarDto> bars = new ArrayList<>();

        for (BudgetDto b : budgets) {
            if (b.getCategory() == null) continue; // Category-specific bars
            bars.add(new BudgetUtilizationBarDto(
                    b.getId(),
                    b.getCategory().getName(),
                    b.getLimitAmount(),
                    b.getSpentAmount(),
                    b.getUtilizationPercent(),
                    b.getStatus()
            ));
        }

        return bars;
    }

    public List<RuleBasedInsightDto> getRuleBasedInsights(Long userId) {
        List<RuleBasedInsightDto> insights = new ArrayList<>();
        LocalDate today = DateUtils.today();
        int year = today.getYear();
        int month = today.getMonthValue();

        // 1. Budget insights
        List<BudgetDto> budgets = budgetService.getBudgetsForMonth(userId, year, month);
        for (BudgetDto b : budgets) {
            if (b.getCategory() == null) continue;
            BigDecimal util = b.getUtilizationPercent();
            if (util != null && util.compareTo(BigDecimal.valueOf(100)) >= 0) {
                insights.add(new RuleBasedInsightDto(
                        "CRITICAL",
                        "Budget Exceeded",
                        String.format("You have exceeded your %s budget (spent %s%%).",
                                b.getCategory().getName(),
                                util.setScale(0, RoundingMode.HALF_UP)),
                        b.getCategory().getName()
                ));
            } else if (util != null && util.compareTo(BigDecimal.valueOf(80)) >= 0) {
                insights.add(new RuleBasedInsightDto(
                        "WARNING",
                        "Budget Approaching Limit",
                        String.format("You've spent %s%% of your %s budget with %d days remaining this month.",
                                util.setScale(0, RoundingMode.HALF_UP),
                                b.getCategory().getName(),
                                YearMonth.from(today).lengthOfMonth() - today.getDayOfMonth()),
                        b.getCategory().getName()
                ));
            }
        }

        // 2. Goal milestones
        List<SavingsGoalDto> goals = goalService.getActiveGoals(userId);
        for (SavingsGoalDto g : goals) {
            BigDecimal completion = g.getCompletionPercent();
            if (completion != null && completion.compareTo(BigDecimal.valueOf(75)) >= 0
                    && completion.compareTo(BigDecimal.valueOf(100)) < 0) {
                insights.add(new RuleBasedInsightDto(
                        "TIP",
                        "Goal Milestone Near",
                        String.format("Your '%s' goal is %s%% funded! You are only ₹%s away from reaching your target.",
                                g.getName(),
                                completion.setScale(0, RoundingMode.HALF_UP),
                                g.getRemainingAmount()),
                        "Savings"
                ));
            }
        }

        // 3. Weekend vs Weekday spending insight
        LocalDate startOfMonth = today.withDayOfMonth(1);
        List<Transaction> monthTx = transactionRepository.findByUserIdAndTransactionDateBetween(userId, startOfMonth, today);
        BigDecimal weekendSpend = BigDecimal.ZERO;
        BigDecimal totalSpend = BigDecimal.ZERO;

        for (Transaction t : monthTx) {
            if (t.getType() == TransactionType.EXPENSE) {
                totalSpend = totalSpend.add(t.getAmount());
                if (DateUtils.isWeekend(t.getTransactionDate())) {
                    weekendSpend = weekendSpend.add(t.getAmount());
                }
            }
        }

        if (totalSpend.compareTo(BigDecimal.valueOf(500)) > 0) {
            BigDecimal weekendPct = weekendSpend.divide(totalSpend, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP);
            if (weekendPct.compareTo(BigDecimal.valueOf(40)) >= 0) {
                insights.add(new RuleBasedInsightDto(
                        "INFO",
                        "Weekend Spending Pattern",
                        String.format("Weekend spending accounts for %s%% of your total expenses so far this month.", weekendPct),
                        "Spending Habits"
                ));
            }
        }

        // 4. Default welcoming insight if empty
        if (insights.isEmpty()) {
            insights.add(new RuleBasedInsightDto(
                    "SUCCESS",
                    "Spending On Track",
                    "Your expenses are well within budget limits this month. Keep up the disciplined saving!",
                    "General"
            ));
        }

        return insights;
    }

    public GroupSummaryDto getGroupSummary(Long userId) {
        List<ExpenseGroup> groups = groupRepository.findActiveGroupsByUserId(userId);
        BigDecimal totalOwedToUser = BigDecimal.ZERO;
        BigDecimal totalUserOwes = BigDecimal.ZERO;

        for (ExpenseGroup group : groups) {
            GroupBalancesDto balances = balanceEngine.computeGroupBalances(group.getId());
            for (MemberBalanceDto member : balances.getMembers()) {
                if (member.getUserId().equals(userId)) {
                    if (member.getNetDecimal().compareTo(BigDecimal.ZERO) > 0) {
                        totalOwedToUser = totalOwedToUser.add(member.getNetDecimal());
                    } else if (member.getNetDecimal().compareTo(BigDecimal.ZERO) < 0) {
                        totalUserOwes = totalUserOwes.add(member.getNetDecimal().abs());
                    }
                }
            }
        }

        BigDecimal net = totalOwedToUser.subtract(totalUserOwes);
        return new GroupSummaryDto(
                MoneyUtils.formatIndian(totalOwedToUser),
                MoneyUtils.formatIndian(totalUserOwes),
                MoneyUtils.formatIndian(net),
                groups.size()
        );
    }
}
