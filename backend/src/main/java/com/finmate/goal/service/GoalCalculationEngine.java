package com.finmate.goal.service;

import com.finmate.common.util.DateUtils;
import com.finmate.goal.dto.SavingsGoalDto;
import com.finmate.goal.entity.SavingsGoal;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class GoalCalculationEngine {

    public SavingsGoalDto calculateGoalMetrics(SavingsGoal goal, BigDecimal saved) {
        SavingsGoalDto dto = new SavingsGoalDto();
        dto.setId(goal.getId());
        dto.setName(goal.getName());
        dto.setTargetAmount(goal.getTargetAmount().toPlainString());
        dto.setTargetDate(goal.getTargetDate());
        dto.setStatus(goal.getStatus());
        dto.setNote(goal.getNote());
        dto.setIcon(goal.getIcon());
        dto.setCreatedDate(goal.getCreatedDate());
        dto.setCompletedAt(goal.getCompletedAt());
        dto.setVersion(goal.getVersion());

        BigDecimal safeSaved = saved != null ? saved : BigDecimal.ZERO;
        dto.setSavedAmount(safeSaved.setScale(2, RoundingMode.HALF_UP).toPlainString());

        BigDecimal target = goal.getTargetAmount();
        BigDecimal remaining = target.subtract(safeSaved);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            remaining = BigDecimal.ZERO;
        }
        remaining = remaining.setScale(2, RoundingMode.HALF_UP);
        dto.setRemainingAmount(remaining.toPlainString());

        // Completion percentage: min(100, saved / target * 100) at scale 2, HALF_UP
        BigDecimal completionPercent = BigDecimal.ZERO.setScale(2);
        if (target.compareTo(BigDecimal.ZERO) > 0) {
            completionPercent = safeSaved.multiply(BigDecimal.valueOf(100))
                    .divide(target, 2, RoundingMode.HALF_UP);
            if (completionPercent.compareTo(BigDecimal.valueOf(100)) > 0) {
                completionPercent = BigDecimal.valueOf(100).setScale(2);
            }
        }
        dto.setCompletionPercent(completionPercent);

        // Target date calculations
        LocalDate today = DateUtils.today();
        long daysUntilTarget = ChronoUnit.DAYS.between(today, goal.getTargetDate());
        dto.setDaysLeft(Math.max(0, daysUntilTarget));

        // Required monthly saving: remaining / monthsLeft, rounded up (CEILING) at scale 2
        // where monthsLeft = max(1, ceil(daysUntilTarget / 30.0))
        long monthsLeft = 1;
        if (daysUntilTarget > 0) {
            monthsLeft = (long) Math.ceil(daysUntilTarget / 30.0);
            if (monthsLeft < 1) monthsLeft = 1;
        }

        BigDecimal requiredMonthly;
        if (remaining.compareTo(BigDecimal.ZERO) <= 0 || daysUntilTarget <= 0) {
            requiredMonthly = BigDecimal.ZERO.setScale(2);
        } else {
            requiredMonthly = remaining.divide(BigDecimal.valueOf(monthsLeft), 2, RoundingMode.CEILING);
        }
        dto.setRequiredMonthlySaving(requiredMonthly.toPlainString());

        // On-track: saved >= target * (elapsedDays / totalDays)
        long totalDays = ChronoUnit.DAYS.between(goal.getCreatedDate(), goal.getTargetDate());
        long elapsedDays = ChronoUnit.DAYS.between(goal.getCreatedDate(), today);

        if (totalDays <= 0 || elapsedDays <= 0) {
            dto.setOnTrack(true);
        } else if (elapsedDays >= totalDays) {
            dto.setOnTrack(safeSaved.compareTo(target) >= 0);
        } else {
            BigDecimal expectedSaved = target.multiply(BigDecimal.valueOf(elapsedDays))
                    .divide(BigDecimal.valueOf(totalDays), 2, RoundingMode.HALF_UP);
            dto.setOnTrack(safeSaved.compareTo(expectedSaved) >= 0);
        }

        return dto;
    }
}
