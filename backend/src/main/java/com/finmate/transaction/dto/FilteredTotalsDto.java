package com.finmate.transaction.dto;

import java.math.BigDecimal;

public class FilteredTotalsDto {
    private String income;
    private String expense;
    private String net;

    public FilteredTotalsDto() {}

    public FilteredTotalsDto(BigDecimal income, BigDecimal expense, BigDecimal net) {
        this.income = income != null ? income.toPlainString() : "0.00";
        this.expense = expense != null ? expense.toPlainString() : "0.00";
        this.net = net != null ? net.toPlainString() : "0.00";
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
