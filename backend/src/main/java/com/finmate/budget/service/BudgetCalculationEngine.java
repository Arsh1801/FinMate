package com.finmate.budget.service;

import com.finmate.budget.dto.BudgetDto;
import com.finmate.budget.entity.Budget;
import com.finmate.budget.entity.BudgetStatus;
import com.finmate.category.dto.CategoryDto;
import com.finmate.common.util.MoneyUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class BudgetCalculationEngine {

    public BudgetDto calculateBudgetMetrics(Budget budget, BigDecimal spent) {
        BudgetDto dto = new BudgetDto();
        dto.setId(budget.getId());
        dto.setBudgetYear(budget.getBudgetYear());
        dto.setBudgetMonth(budget.getBudgetMonth());
        dto.setCategory(budget.getCategory() != null ? CategoryDto.fromEntity(budget.getCategory()) : null);
        dto.setLimitAmount(budget.getLimitAmount().toPlainString());
        dto.setAlertThreshold(budget.getAlertThreshold());
        dto.setLastAlertLevel(budget.getLastAlertLevel());
        dto.setVersion(budget.getVersion());

        BigDecimal safeSpent = spent != null ? spent : BigDecimal.ZERO;
        dto.setSpentAmount(safeSpent.setScale(2, RoundingMode.HALF_UP).toPlainString());

        BigDecimal limit = budget.getLimitAmount();
        BigDecimal remaining = limit.subtract(safeSpent).setScale(2, RoundingMode.HALF_UP);
        dto.setRemainingAmount(remaining.toPlainString());

        BigDecimal utilization = BigDecimal.ZERO.setScale(2);
        if (limit.compareTo(BigDecimal.ZERO) > 0) {
            utilization = safeSpent.multiply(BigDecimal.valueOf(100))
                    .divide(limit, 2, RoundingMode.HALF_UP);
        }
        dto.setUtilizationPercent(utilization);

        int threshold = budget.getAlertThreshold() != null ? budget.getAlertThreshold() : 80;

        if (utilization.compareTo(BigDecimal.valueOf(100)) > 0) {
            dto.setStatus(BudgetStatus.EXCEEDED);
            dto.setExceeded(true);
            BigDecimal overspent = safeSpent.subtract(limit).setScale(2, RoundingMode.HALF_UP);
            dto.setOverspentAmount(overspent.toPlainString());
        } else if (utilization.compareTo(BigDecimal.valueOf(threshold)) >= 0) {
            dto.setStatus(BudgetStatus.WARNING);
            dto.setExceeded(false);
            dto.setOverspentAmount("0.00");
        } else {
            dto.setStatus(BudgetStatus.SAFE);
            dto.setExceeded(false);
            dto.setOverspentAmount("0.00");
        }

        return dto;
    }
}
