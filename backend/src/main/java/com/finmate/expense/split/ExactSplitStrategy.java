package com.finmate.expense.split;

import com.finmate.common.exception.ApiException;
import com.finmate.common.exception.ErrorCode;
import com.finmate.common.util.MoneyUtils;
import com.finmate.expense.entity.SplitType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class ExactSplitStrategy implements SplitStrategy {

    @Override
    public SplitType getSupportedType() {
        return SplitType.EXACT;
    }

    @Override
    public List<CalculatedSplitResult> calculateSplits(BigDecimal totalAmount, List<SplitItemInput> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Participants list cannot be empty");
        }

        long totalPaise = MoneyUtils.toPaise(totalAmount);
        long sumPaise = 0;
        List<CalculatedSplitResult> results = new ArrayList<>();

        for (SplitItemInput input : inputs) {
            if (input.getAmount() == null || input.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw ApiException.businessRule(ErrorCode.SPLIT_SHARE_TOO_SMALL,
                        "Exact share for user " + input.getUserId() + " must be strictly positive (min ₹0.01)");
            }
            long itemPaise = MoneyUtils.toPaise(input.getAmount());
            sumPaise += itemPaise;
            BigDecimal share = MoneyUtils.fromPaise(itemPaise);
            BigDecimal percentage = MoneyUtils.calculatePercentage(share, totalAmount);
            results.add(new CalculatedSplitResult(input.getUserId(), share, percentage));
        }

        if (sumPaise != totalPaise) {
            BigDecimal sumAmount = MoneyUtils.fromPaise(sumPaise);
            BigDecimal difference = totalAmount.subtract(sumAmount);
            throw ApiException.businessRule(ErrorCode.SPLIT_SUM_MISMATCH,
                    String.format("Exact shares sum (%s) does not match total expense amount (%s). Difference: %s",
                            sumAmount, totalAmount, difference.abs()));
        }

        return results;
    }
}
