package com.finmate.budget.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class UpdateBudgetRequest {

    @NotNull(message = "Limit amount is required")
    @DecimalMin(value = "0.01", message = "Limit amount must be strictly positive")
    @DecimalMax(value = "99999999.99", message = "Limit amount cannot exceed 99,999,999.99")
    @Digits(integer = 8, fraction = 2, message = "Limit amount cannot have more than 2 decimal places")
    private BigDecimal limitAmount;

    @Min(value = 50, message = "Alert threshold must be between 50 and 99")
    @Max(value = 99, message = "Alert threshold must be between 50 and 99")
    private Integer alertThreshold = 80;

    @NotNull(message = "Version is required for optimistic locking check")
    private Integer version;

    public BigDecimal getLimitAmount() {
        return limitAmount;
    }

    public void setLimitAmount(BigDecimal limitAmount) {
        this.limitAmount = limitAmount;
    }

    public Integer getAlertThreshold() {
        return alertThreshold;
    }

    public void setAlertThreshold(Integer alertThreshold) {
        this.alertThreshold = alertThreshold;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
