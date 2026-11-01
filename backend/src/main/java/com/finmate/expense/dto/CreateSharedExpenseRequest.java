package com.finmate.expense.dto;

import com.finmate.expense.entity.SplitType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class CreateSharedExpenseRequest {

    @NotNull(message = "Expense amount is required")
    @DecimalMin(value = "0.01", message = "Expense amount must be strictly positive")
    @DecimalMax(value = "99999999.99", message = "Expense amount cannot exceed 99,999,999.99")
    @Digits(integer = 8, fraction = 2, message = "Expense amount cannot have more than 2 decimal places")
    private BigDecimal amount;

    @NotBlank(message = "Description is required")
    @Size(min = 3, max = 100, message = "Description must be between 3 and 100 characters")
    private String description;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;

    @NotNull(message = "Expense date is required")
    private LocalDate expenseDate;

    @NotNull(message = "Payer is required")
    private Long paidBy;

    private Long categoryId;

    @NotNull(message = "Split type is required")
    private SplitType splitType = SplitType.EQUAL;

    @NotEmpty(message = "At least one participant is required")
    @Size(min = 1, max = 20, message = "Number of participants must be between 1 and 20")
    private List<SplitItemRequest> splits;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description != null ? description.trim() : null;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    public Long getPaidBy() {
        return paidBy;
    }

    public void setPaidBy(Long paidBy) {
        this.paidBy = paidBy;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public SplitType getSplitType() {
        return splitType;
    }

    public void setSplitType(SplitType splitType) {
        this.splitType = splitType;
    }

    public List<SplitItemRequest> getSplits() {
        return splits;
    }

    public void setSplits(List<SplitItemRequest> splits) {
        this.splits = splits;
    }
}
