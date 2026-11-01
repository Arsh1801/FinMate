package com.finmate.budget.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class CreateBudgetRequest {

    @NotNull(message = "Year is required")
    @Min(value = 2000, message = "Year must be 2000 or later")
    @Max(value = 2100, message = "Year cannot exceed 2100")
    private Integer year;

    @NotNull(message = "Month is required")
    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    private Integer month;

    private Long categoryId; // Optional; null = overall budget

    @NotNull(message = "Limit amount is required")
    @DecimalMin(value = "0.01", message = "Limit amount must be strictly positive")
    @DecimalMax(value = "99999999.99", message = "Limit amount cannot exceed 99,999,999.99")
    @Digits(integer = 8, fraction = 2, message = "Limit amount cannot have more than 2 decimal places")
    private BigDecimal limitAmount;

    @Min(value = 50, message = "Alert threshold must be between 50 and 99")
    @Max(value = 99, message = "Alert threshold must be between 50 and 99")
    private Integer alertThreshold = 80;

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

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
}
