package com.finmate.expense.split;

import java.math.BigDecimal;

public class CalculatedSplitResult {
    private Long userId;
    private BigDecimal shareAmount;
    private BigDecimal percentage;

    public CalculatedSplitResult() {}

    public CalculatedSplitResult(Long userId, BigDecimal shareAmount, BigDecimal percentage) {
        this.userId = userId;
        this.shareAmount = shareAmount;
        this.percentage = percentage;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getShareAmount() {
        return shareAmount;
    }

    public void setShareAmount(BigDecimal shareAmount) {
        this.shareAmount = shareAmount;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }
}
