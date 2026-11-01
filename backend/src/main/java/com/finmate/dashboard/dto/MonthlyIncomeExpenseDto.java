package com.finmate.dashboard.dto;

public class MonthlyIncomeExpenseDto {
    private String month; // YYYY-MM
    private String income;
    private String expense;
    private String net;

    public MonthlyIncomeExpenseDto() {}

    public MonthlyIncomeExpenseDto(String month, String income, String expense, String net) {
        this.month = month;
        this.income = income;
        this.expense = expense;
        this.net = net;
    }

    public MonthlyIncomeExpenseDto(String month, java.math.BigDecimal income, java.math.BigDecimal expense, java.math.BigDecimal net) {
        this(month,
             income != null ? income.toPlainString() : "0.00",
             expense != null ? expense.toPlainString() : "0.00",
             net != null ? net.toPlainString() : "0.00");
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public String getIncome() {
        return income;
    }

    public void setIncome(String income) {
        this.income = income;
    }

    public String getExpense() {
        return expense;
    }

    public void setExpense(String expense) {
        this.expense = expense;
    }

    public String getNet() {
        return net;
    }

    public void setNet(String net) {
        this.net = net;
    }
}
