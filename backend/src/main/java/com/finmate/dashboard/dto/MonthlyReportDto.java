package com.finmate.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

public class MonthlyReportDto {
    private String month;
    private String totalIncome;
    private String totalExpense;
    private String netSavings;
    private BigDecimal savingsRate;
    private List<CategoryBreakdownItem> categoryBreakdown;
    private List<BudgetPerformanceItem> budgetPerformance;
    private List<GoalProgressItem> goalsSummary;
    private String generatedAt;

    public static class CategoryBreakdownItem {
        private String categoryName;
        private String categoryType;
        private String amount;
        private BigDecimal percentage;

        public CategoryBreakdownItem() {}

        public CategoryBreakdownItem(String categoryName, String categoryType, String amount, BigDecimal percentage) {
            this.categoryName = categoryName;
            this.categoryType = categoryType;
            this.amount = amount;
            this.percentage = percentage;
        }

        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        public String getCategoryType() { return categoryType; }
        public void setCategoryType(String categoryType) { this.categoryType = categoryType; }
        public String getAmount() { return amount; }
        public void setAmount(String amount) { this.amount = amount; }
        public BigDecimal getPercentage() { return percentage; }
        public void setPercentage(BigDecimal percentage) { this.percentage = percentage; }
    }

    public static class BudgetPerformanceItem {
        private String categoryName;
        private String budgetAmount;
        private String spentAmount;
        private String remainingAmount;
        private BigDecimal percentageUsed;
        private String status;

        public BudgetPerformanceItem() {}

        public BudgetPerformanceItem(String categoryName, String budgetAmount, String spentAmount, String remainingAmount, BigDecimal percentageUsed, String status) {
            this.categoryName = categoryName;
            this.budgetAmount = budgetAmount;
            this.spentAmount = spentAmount;
            this.remainingAmount = remainingAmount;
            this.percentageUsed = percentageUsed;
            this.status = status;
        }

        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        public String getBudgetAmount() { return budgetAmount; }
        public void setBudgetAmount(String budgetAmount) { this.budgetAmount = budgetAmount; }
        public String getSpentAmount() { return spentAmount; }
        public void setSpentAmount(String spentAmount) { this.spentAmount = spentAmount; }
        public String getRemainingAmount() { return remainingAmount; }
        public void setRemainingAmount(String remainingAmount) { this.remainingAmount = remainingAmount; }
        public BigDecimal getPercentageUsed() { return percentageUsed; }
        public void setPercentageUsed(BigDecimal percentageUsed) { this.percentageUsed = percentageUsed; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class GoalProgressItem {
        private String goalName;
        private String targetAmount;
        private String currentAmount;
        private BigDecimal progressPercentage;
        private String targetDate;

        public GoalProgressItem() {}

        public GoalProgressItem(String goalName, String targetAmount, String currentAmount, BigDecimal progressPercentage, String targetDate) {
            this.goalName = goalName;
            this.targetAmount = targetAmount;
            this.currentAmount = currentAmount;
            this.progressPercentage = progressPercentage;
            this.targetDate = targetDate;
        }

        public String getGoalName() { return goalName; }
        public void setGoalName(String goalName) { this.goalName = goalName; }
        public String getTargetAmount() { return targetAmount; }
        public void setTargetAmount(String targetAmount) { this.targetAmount = targetAmount; }
        public String getCurrentAmount() { return currentAmount; }
        public void setCurrentAmount(String currentAmount) { this.currentAmount = currentAmount; }
        public BigDecimal getProgressPercentage() { return progressPercentage; }
        public void setProgressPercentage(BigDecimal progressPercentage) { this.progressPercentage = progressPercentage; }
        public String getTargetDate() { return targetDate; }
        public void setTargetDate(String targetDate) { this.targetDate = targetDate; }
    }

    public MonthlyReportDto() {}

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }
    public String getTotalIncome() { return totalIncome; }
    public void setTotalIncome(String totalIncome) { this.totalIncome = totalIncome; }
    public String getTotalExpense() { return totalExpense; }
    public void setTotalExpense(String totalExpense) { this.totalExpense = totalExpense; }
    public String getNetSavings() { return netSavings; }
    public void setNetSavings(String netSavings) { this.netSavings = netSavings; }
    public BigDecimal getSavingsRate() { return savingsRate; }
    public void setSavingsRate(BigDecimal savingsRate) { this.savingsRate = savingsRate; }
    public List<CategoryBreakdownItem> getCategoryBreakdown() { return categoryBreakdown; }
    public void setCategoryBreakdown(List<CategoryBreakdownItem> categoryBreakdown) { this.categoryBreakdown = categoryBreakdown; }
    public List<BudgetPerformanceItem> getBudgetPerformance() { return budgetPerformance; }
    public void setBudgetPerformance(List<BudgetPerformanceItem> budgetPerformance) { this.budgetPerformance = budgetPerformance; }
    public List<GoalProgressItem> getGoalsSummary() { return goalsSummary; }
    public void setGoalsSummary(List<GoalProgressItem> goalsSummary) { this.goalsSummary = goalsSummary; }
    public String getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(String generatedAt) { this.generatedAt = generatedAt; }
}
