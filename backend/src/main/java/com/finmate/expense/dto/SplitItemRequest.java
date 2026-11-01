package com.finmate.expense.dto;

import java.math.BigDecimal;

public class SplitItemRequest {
    private Long userId;
    private BigDecimal amount;
    private BigDecimal percentage;

    public SplitItemRequest() {}

    public SplitItemRequest(Long userId, BigDecimal amount, BigDecimal percentage) {
        this.userId = userId;
        this.amount = amount;
        this.percentage = percentage;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }
}
