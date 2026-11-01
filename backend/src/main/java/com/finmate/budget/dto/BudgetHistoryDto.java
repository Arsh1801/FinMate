package com.finmate.budget.dto;

import com.finmate.budget.entity.BudgetStatus;
import com.finmate.category.dto.CategoryDto;

import java.math.BigDecimal;

public class BudgetHistoryDto {
    private Integer budgetYear;
    private Integer budgetMonth;
    private CategoryDto category;
    private String limitAmount;
    private String spentAmount;
    private String remainingAmount;
    private BigDecimal utilizationPercent;
    private BudgetStatus status;

    public BudgetHistoryDto() {}

    public BudgetHistoryDto(Integer budgetYear, Integer budgetMonth, CategoryDto category,
                            String limitAmount, String spentAmount, String remainingAmount,
                            BigDecimal utilizationPercent, BudgetStatus status) {
        this.budgetYear = budgetYear;
        this.budgetMonth = budgetMonth;
        this.category = category;
        this.limitAmount = limitAmount;
        this.spentAmount = spentAmount;
        this.remainingAmount = remainingAmount;
        this.utilizationPercent = utilizationPercent;
        this.status = status;
    }

    public Integer getBudgetYear() {
        return budgetYear;
    }

    public void setBudgetYear(Integer budgetYear) {
        this.budgetYear = budgetYear;
    }

    public Integer getBudgetMonth() {
        return budgetMonth;
    }

    public void setBudgetMonth(Integer budgetMonth) {
        this.budgetMonth = budgetMonth;
    }

    public CategoryDto getCategory() {
        return category;
    }

    public void setCategory(CategoryDto category) {
        this.category = category;
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

    public String getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(String remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public BigDecimal getUtilizationPercent() {
        return utilizationPercent;
    }

    public void setUtilizationPercent(BigDecimal utilizationPercent) {
        this.utilizationPercent = utilizationPercent;
    }

    public BudgetStatus getStatus() {
        return status;
    }

    public void setStatus(BudgetStatus status) {
        this.status = status;
    }
}
