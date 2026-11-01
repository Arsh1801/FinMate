package com.finmate.dashboard.dto;

import java.math.BigDecimal;
import java.util.Map;

public class DashboardSummaryDto {
    private String totalIncome;
    private String totalExpense;
    private String currentBalance;
    private String totalSavings;
    private String freeBalance;
    private String netSavings;
    private BigDecimal savingsRate;
    private String avgDailySpend;
    private String projectedMonthEnd;
    private Map<String, String> deltas; // delta % for income, expense, savings, etc.

    public DashboardSummaryDto() {}

    public String getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(String totalIncome) {
        this.totalIncome = totalIncome;
    }

    public String getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(String totalExpense) {
        this.totalExpense = totalExpense;
    }

    public String getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(String currentBalance) {
        this.currentBalance = currentBalance;
    }

    public String getTotalSavings() {
        return totalSavings;
    }

    public void setTotalSavings(String totalSavings) {
        this.totalSavings = totalSavings;
    }

    public String getFreeBalance() {
        return freeBalance;
    }

    public void setFreeBalance(String freeBalance) {
        this.freeBalance = freeBalance;
    }

    public String getNetSavings() {
        return netSavings;
    }

    public void setNetSavings(String netSavings) {
        this.netSavings = netSavings;
    }

    public BigDecimal getSavingsRate() {
        return savingsRate;
    }

    public void setSavingsRate(BigDecimal savingsRate) {
        this.savingsRate = savingsRate;
    }

    public String getAvgDailySpend() {
        return avgDailySpend;
    }

    public void setAvgDailySpend(String avgDailySpend) {
        this.avgDailySpend = avgDailySpend;
    }

    public String getProjectedMonthEnd() {
        return projectedMonthEnd;
    }

    public void setProjectedMonthEnd(String projectedMonthEnd) {
        this.projectedMonthEnd = projectedMonthEnd;
    }

    public Map<String, String> getDeltas() {
        return deltas;
    }

    public void setDeltas(Map<String, String> deltas) {
        this.deltas = deltas;
    }
}
