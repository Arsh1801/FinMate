package com.finmate.expense.dto;

import com.finmate.expense.entity.ExpenseSplit;

import java.math.BigDecimal;

public class ExpenseSplitDto {
    private Long userId;
    private String fullName;
    private String email;
    private String shareAmount;
    private BigDecimal percentage;

    public ExpenseSplitDto() {}

    public static ExpenseSplitDto fromEntity(ExpenseSplit split) {
        ExpenseSplitDto dto = new ExpenseSplitDto();
        dto.setUserId(split.getUser().getId());
        dto.setFullName(split.getUser().getFullName());
        dto.setEmail(split.getUser().getEmail());
        dto.setShareAmount(split.getShareAmount() != null ? split.getShareAmount().toPlainString() : "0.00");
        dto.setPercentage(split.getPercentage());
        return dto;
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

    public String getShareAmount() {
        return shareAmount;
    }

    public void setShareAmount(String shareAmount) {
        this.shareAmount = shareAmount;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }
}
