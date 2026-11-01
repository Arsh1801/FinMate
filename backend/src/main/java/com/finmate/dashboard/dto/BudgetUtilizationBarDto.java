package com.finmate.dashboard.dto;

import com.finmate.budget.entity.BudgetStatus;

import java.math.BigDecimal;

public class BudgetUtilizationBarDto {
    private Long budgetId;
    private String categoryName;
    private String limitAmount;
    private String spentAmount;
    private BigDecimal utilization;
    private BudgetStatus status;

    public BudgetUtilizationBarDto() {}

    public BudgetUtilizationBarDto(Long budgetId, String categoryName, String limitAmount, String spentAmount, BigDecimal utilization, BudgetStatus status) {
        this.budgetId = budgetId;
        this.categoryName = categoryName;
        this.limitAmount = limitAmount;
        this.spentAmount = spentAmount;
        this.utilization = utilization;
        this.status = status;
    }

    public Long getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(Long budgetId) {
        this.budgetId = budgetId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getLimitAmount() {
        return limitAmount;
    }

    public void setLimitAmount(String limitAmount) {
        this.limitAmount = limitAmount;
    }

    public String getSpentAmount() {
        return spentAmount;
    }

    public void setSpentAmount(String spentAmount) {
        this.spentAmount = spentAmount;
    }

    public BigDecimal getUtilization() {
        return utilization;
    }

    public void setUtilization(BigDecimal utilization) {
        this.utilization = utilization;
    }

    public BudgetStatus getStatus() {
        return status;
    }

    public void setStatus(BudgetStatus status) {
        this.status = status;
    }
}
