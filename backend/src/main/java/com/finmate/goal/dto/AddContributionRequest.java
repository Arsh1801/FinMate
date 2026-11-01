package com.finmate.goal.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AddContributionRequest {

    @NotNull(message = "Contribution amount is required")
    @DecimalMin(value = "0.01", message = "Contribution amount must be strictly positive")
    @DecimalMax(value = "99999999.99", message = "Contribution amount cannot exceed 99,999,999.99")
    @Digits(integer = 8, fraction = 2, message = "Contribution amount cannot have more than 2 decimal places")
    private BigDecimal amount;

    @NotNull(message = "Contribution date is required")
    private LocalDate contributionDate;

    @Size(max = 255, message = "Note cannot exceed 255 characters")
    private String note;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getContributionDate() {
        return contributionDate;
    }

    public void setContributionDate(LocalDate contributionDate) {
        this.contributionDate = contributionDate;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
