package com.finmate.expense.split;

import com.finmate.common.exception.ApiException;
import com.finmate.common.exception.ErrorCode;
import com.finmate.common.util.MoneyUtils;
import com.finmate.expense.entity.SplitType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class PercentageSplitStrategy implements SplitStrategy {

    private static class ParticipantRemainder {
        final Long userId;
        final BigDecimal percentage;
        final long floorPaise;
        final double remainder;

        ParticipantRemainder(Long userId, BigDecimal percentage, long floorPaise, double remainder) {
            this.userId = userId;
            this.percentage = percentage;
            this.floorPaise = floorPaise;
            this.remainder = remainder;
        }
    }

    @Override
    public SplitType getSupportedType() {
        return SplitType.PERCENTAGE;
    }

    @Override
    public List<CalculatedSplitResult> calculateSplits(BigDecimal totalAmount, List<SplitItemInput> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Participants list cannot be empty");
        }

        long totalPaise = MoneyUtils.toPaise(totalAmount);
        BigDecimal pctSum = BigDecimal.ZERO;

        for (SplitItemInput input : inputs) {
            if (input.getPercentage() == null || input.getPercentage().compareTo(BigDecimal.ZERO) <= 0) {
                throw ApiException.businessRule(ErrorCode.SPLIT_SHARE_TOO_SMALL,
                        "Percentage for user " + input.getUserId() + " must be strictly positive");
            }
            pctSum = pctSum.add(input.getPercentage());
        }

        if (pctSum.setScale(2, RoundingMode.HALF_UP).compareTo(BigDecimal.valueOf(100).setScale(2)) != 0) {
            throw ApiException.businessRule(ErrorCode.SPLIT_SUM_MISMATCH,
                    "Split percentages must sum to exactly 100.00%. Current sum: " + pctSum);
        }

        List<ParticipantRemainder> remainders = new ArrayList<>();
        long sumFloorPaise = 0;

        for (SplitItemInput input : inputs) {
            double pct = input.getPercentage().doubleValue();
            double raw = totalPaise * pct / 100.0;
            long floor = (long) Math.floor(raw);
            double rem = raw - floor;
            remainders.add(new ParticipantRemainder(input.getUserId(), input.getPercentage(), floor, rem));
            sumFloorPaise += floor;
        }

        long r = totalPaise - sumFloorPaise;

        // Sort by remainder descending, tie break by lowest userId ascending
        List<ParticipantRemainder> sorted = new ArrayList<>(remainders);
        sorted.sort((a, b) -> {
            int cmp = Double.compare(b.remainder, a.remainder);
            if (cmp != 0) return cmp;
            return a.userId.compareTo(b.userId);
        });

        java.util.Set<Long> recipientsOfBonusPaisa = new java.util.HashSet<>();
        for (int i = 0; i < r && i < sorted.size(); i++) {
            recipientsOfBonusPaisa.add(sorted.get(i).userId);
        }

        List<CalculatedSplitResult> results = new ArrayList<>();
        long calculatedTotalPaise = 0;

        for (ParticipantRemainder pr : remainders) {
            long allocatedPaise = pr.floorPaise + (recipientsOfBonusPaisa.contains(pr.userId) ? 1 : 0);
            if (allocatedPaise < 1) {
                throw ApiException.businessRule(ErrorCode.SPLIT_SHARE_TOO_SMALL,
                        "Share for user " + pr.userId + " rounds to less than 1 paisa (₹0.01)");
            }
            calculatedTotalPaise += allocatedPaise;
            BigDecimal share = MoneyUtils.fromPaise(allocatedPaise);
            results.add(new CalculatedSplitResult(pr.userId, share, pr.percentage.setScale(2, RoundingMode.HALF_UP)));
        }

        if (calculatedTotalPaise != totalPaise) {
            throw ApiException.businessRule(ErrorCode.SPLIT_SUM_MISMATCH, "Internal error in percentage split calculation");
        }

        return results;
    }
}
