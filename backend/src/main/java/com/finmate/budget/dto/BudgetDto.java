package com.finmate.budget.dto;

import com.finmate.budget.entity.BudgetAlertLevel;
import com.finmate.budget.entity.BudgetStatus;
import com.finmate.category.dto.CategoryDto;

import java.math.BigDecimal;

public class BudgetDto {
    private Long id;
    private Integer budgetYear;
    private Integer budgetMonth;
    private CategoryDto category; // null means overall budget
    private String limitAmount;
    private String spentAmount;
    private String remainingAmount;
    private BigDecimal utilizationPercent;
    private Integer alertThreshold;
    private BudgetStatus status;
    private boolean exceeded;
    private String overspentAmount;
    private BudgetAlertLevel lastAlertLevel;
    private Integer version;

    public BudgetDto() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getAlertThreshold() {
        return alertThreshold;
    }

    public void setAlertThreshold(Integer alertThreshold) {
        this.alertThreshold = alertThreshold;
    }

    public BudgetStatus getStatus() {
        return status;
    }

    public void setStatus(BudgetStatus status) {
        this.status = status;
    }

    public boolean isExceeded() {
        return exceeded;
    }

    public void setExceeded(boolean exceeded) {
        this.exceeded = exceeded;
    }

    public String getOverspentAmount() {
        return overspentAmount;
    }

    public void setOverspentAmount(String overspentAmount) {
        this.overspentAmount = overspentAmount;
    }

    public BudgetAlertLevel getLastAlertLevel() {
        return lastAlertLevel;
    }

    public void setLastAlertLevel(BudgetAlertLevel lastAlertLevel) {
        this.lastAlertLevel = lastAlertLevel;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
