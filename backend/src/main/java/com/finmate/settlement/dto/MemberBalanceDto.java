package com.finmate.settlement.dto;

import java.math.BigDecimal;

public class MemberBalanceDto {
    private Long userId;
    private String fullName;
    private String email;
    private String paidAmount;
    private String owedAmount;
    private String settledPaid;
    private String settledReceived;
    private String netBalance;
    private BigDecimal netDecimal;

    public MemberBalanceDto() {}

    public MemberBalanceDto(Long userId, String fullName, String email,
                            BigDecimal paid, BigDecimal owed,
                            BigDecimal settledPaid, BigDecimal settledReceived,
                            BigDecimal net) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.paidAmount = paid != null ? paid.toPlainString() : "0.00";
        this.owedAmount = owed != null ? owed.toPlainString() : "0.00";
        this.settledPaid = settledPaid != null ? settledPaid.toPlainString() : "0.00";
        this.settledReceived = settledReceived != null ? settledReceived.toPlainString() : "0.00";
        this.netBalance = net != null ? net.toPlainString() : "0.00";
        this.netDecimal = net != null ? net : BigDecimal.ZERO;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(String paidAmount) {
        this.paidAmount = paidAmount;
    }

    public String getOwedAmount() {
        return owedAmount;
    }

    public void setOwedAmount(String owedAmount) {
        this.owedAmount = owedAmount;
    }

    public String getSettledPaid() {
        return settledPaid;
    }

    public void setSettledPaid(String settledPaid) {
        this.settledPaid = settledPaid;
    }

    public String getSettledReceived() {
        return settledReceived;
    }

    public void setSettledReceived(String settledReceived) {
        this.settledReceived = settledReceived;
    }

    public String getNetBalance() {
        return netBalance;
    }

    public void setNetBalance(String netBalance) {
        this.netBalance = netBalance;
    }

    public BigDecimal getNetDecimal() {
        return netDecimal;
    }

    public void setNetDecimal(BigDecimal netDecimal) {
        this.netDecimal = netDecimal;
    }
}
