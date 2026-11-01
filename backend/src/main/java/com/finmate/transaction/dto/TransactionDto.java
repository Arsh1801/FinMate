package com.finmate.transaction.dto;

import com.finmate.category.dto.CategoryDto;
import com.finmate.transaction.entity.PaymentMethod;
import com.finmate.transaction.entity.Tag;
import com.finmate.transaction.entity.Transaction;
import com.finmate.transaction.entity.TransactionType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class TransactionDto {
    private Long id;
    private TransactionType type;
    private String amount;
    private CategoryDto category;
    private LocalDate transactionDate;
    private String description;
    private String notes;
    private PaymentMethod paymentMethod;
    private Long recurringRuleId;
    private List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer version;

    public TransactionDto() {}

    public static TransactionDto fromEntity(Transaction t) {
        TransactionDto dto = new TransactionDto();
        dto.setId(t.getId());
        dto.setType(t.getType());
        dto.setAmount(t.getAmount() != null ? t.getAmount().toPlainString() : "0.00");
        dto.setCategory(t.getCategory() != null ? CategoryDto.fromEntity(t.getCategory()) : null);
        dto.setTransactionDate(t.getTransactionDate());
        dto.setDescription(t.getDescription());
        dto.setNotes(t.getNotes());
        dto.setPaymentMethod(t.getPaymentMethod());
        dto.setRecurringRuleId(t.getRecurringRule() != null ? t.getRecurringRule().getId() : null);
        if (t.getTags() != null) {
            dto.setTags(t.getTags().stream().map(Tag::getName).collect(Collectors.toList()));
        }
        dto.setCreatedAt(t.getCreatedAt());
        dto.setUpdatedAt(t.getUpdatedAt());
        dto.setVersion(t.getVersion());
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

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Long getRecurringRuleId() {
        return recurringRuleId;
    }

    public void setRecurringRuleId(Long recurringRuleId) {
        this.recurringRuleId = recurringRuleId;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
