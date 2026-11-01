package com.finmate.settlement.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RecordSettlementRequest {

    @NotNull(message = "Payer (fromUserId) is required")
    private Long fromUserId;

    @NotNull(message = "Receiver (toUserId) is required")
    private Long toUserId;

    @NotNull(message = "Settlement amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be strictly positive")
    @DecimalMax(value = "99999999.99", message = "Amount cannot exceed 99,999,999.99")
    @Digits(integer = 8, fraction = 2, message = "Amount cannot have more than 2 decimal places")
    private BigDecimal amount;

    @NotNull(message = "Settlement date is required")
    private LocalDate settlementDate;

    @Size(max = 255, message = "Note cannot exceed 255 characters")
    private String note;

    public Long getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(Long fromUserId) {
        this.fromUserId = fromUserId;
    }

    public Long getToUserId() {
        return toUserId;
    }

    public void setToUserId(Long toUserId) {
        this.toUserId = toUserId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(LocalDate settlementDate) {
        this.settlementDate = settlementDate;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
