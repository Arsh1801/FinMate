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
public class EqualSplitStrategy implements SplitStrategy {

    @Override
    public SplitType getSupportedType() {
        return SplitType.EQUAL;
    }

    @Override
    public List<CalculatedSplitResult> calculateSplits(BigDecimal totalAmount, List<SplitItemInput> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Participants list cannot be empty");
        }

        long totalPaise = MoneyUtils.toPaise(totalAmount);
        int n = inputs.size();

        // C-01: if any share would be 0 paise
        if (totalPaise < n) {
            throw ApiException.businessRule(ErrorCode.SPLIT_SHARE_TOO_SMALL,
                    "Total amount (" + totalAmount + ") is too small to distribute among " + n + " participants (min ₹0.01 per participant)");
        }

        long base = totalPaise / n;
        long r = totalPaise % n;

        // Sort participants by user id ascending (deterministic per BR-006 & Section 6.4)
        List<SplitItemInput> sorted = new ArrayList<>(inputs);
        sorted.sort(Comparator.comparing(SplitItemInput::getUserId));

        List<CalculatedSplitResult> results = new ArrayList<>();
        long runningPaise = 0;

        for (int i = 0; i < n; i++) {
            long sharePaise = (i < r) ? base + 1 : base;
            runningPaise += sharePaise;
            BigDecimal share = MoneyUtils.fromPaise(sharePaise);
            BigDecimal percentage = MoneyUtils.calculatePercentage(share, totalAmount);
            results.add(new CalculatedSplitResult(sorted.get(i).getUserId(), share, percentage));
        }

        // Invariant check
        if (runningPaise != totalPaise) {
            throw ApiException.businessRule(ErrorCode.SPLIT_SUM_MISMATCH, "Internal error in equal split calculation");
        }

        return results;
    }
}
