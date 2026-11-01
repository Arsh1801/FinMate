package com.finmate.dashboard.dto;

import java.math.BigDecimal;

public class SpendingTrendDto {
    private String date; // YYYY-MM-DD or YYYY-MM
    private String label; // e.g. "Oct 12"
    private BigDecimal amount;
    private String expense;

    public SpendingTrendDto() {}

    public SpendingTrendDto(String month, String expense) {
        this.date = month;
        this.label = month;
        this.expense = expense;
        this.amount = expense != null ? new BigDecimal(expense) : BigDecimal.ZERO;
    }

    public SpendingTrendDto(String date, String label, BigDecimal amount) {
        this.date = date;
        this.label = label;
        this.amount = amount;
        this.expense = amount != null ? amount.toPlainString() : "0.00";
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getExpense() {
        return expense;
    }

    public void setExpense(String expense) {
        this.expense = expense;
    }
}
