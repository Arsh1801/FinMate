package com.finmate.settlement.dto;

import java.math.BigDecimal;

public class SettlementSuggestionDto {
    private Long fromUserId;
    private String fromUserName;
    private Long toUserId;
    private String toUserName;
    private String amount;

    public SettlementSuggestionDto() {}

    public SettlementSuggestionDto(Long fromUserId, String fromUserName, Long toUserId, String toUserName, BigDecimal amount) {
        this.fromUserId = fromUserId;
        this.fromUserName = fromUserName;
        this.toUserId = toUserId;
        this.toUserName = toUserName;
        this.amount = amount != null ? amount.toPlainString() : "0.00";
    }

    public Long getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(Long fromUserId) {
        this.fromUserId = fromUserId;
    }

    public String getFromUserName() {
        return fromUserName;
    }

    public void setFromUserName(String fromUserName) {
        this.fromUserName = fromUserName;
    }

    public Long getToUserId() {
        return toUserId;
    }

    public void setToUserId(Long toUserId) {
        this.toUserId = toUserId;
    }

    public String getToUserName() {
        return toUserName;
    }

    public void setToUserName(String toUserName) {
        this.toUserName = toUserName;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }
}
