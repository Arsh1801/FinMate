package com.finmate.transaction.dto;

import com.finmate.category.dto.CategoryDto;
import com.finmate.transaction.entity.PaymentMethod;
import com.finmate.transaction.entity.RecurrenceFrequency;
import com.finmate.transaction.entity.RecurringRule;
import com.finmate.transaction.entity.TransactionType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class RecurringRuleDto {
    private Long id;
    private TransactionType type;
    private String amount;
    private CategoryDto category;
    private String description;
    private PaymentMethod paymentMethod;
    private RecurrenceFrequency frequency;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate nextRunDate;
    private boolean isActive;
    private LocalDateTime createdAt;

    public RecurringRuleDto() {}

    public static RecurringRuleDto fromEntity(RecurringRule r) {
        RecurringRuleDto dto = new RecurringRuleDto();
        dto.setId(r.getId());
        dto.setType(r.getType());
        dto.setAmount(r.getAmount() != null ? r.getAmount().toPlainString() : "0.00");
        dto.setCategory(r.getCategory() != null ? CategoryDto.fromEntity(r.getCategory()) : null);
        dto.setDescription(r.getDescription());
        dto.setPaymentMethod(r.getPaymentMethod());
        dto.setFrequency(r.getFrequency());
        dto.setStartDate(r.getStartDate());
        dto.setEndDate(r.getEndDate());
        dto.setNextRunDate(r.getNextRunDate());
        dto.setActive(Boolean.TRUE.equals(r.getIsActive()));
        dto.setCreatedAt(r.getCreatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public CategoryDto getCategory() {
        return category;
    }

    public void setCategory(CategoryDto category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public RecurrenceFrequency getFrequency() {
        return frequency;
    }

    public void setFrequency(RecurrenceFrequency frequency) {
        this.frequency = frequency;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDate getNextRunDate() {
        return nextRunDate;
    }

    public void setNextRunDate(LocalDate nextRunDate) {
        this.nextRunDate = nextRunDate;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
