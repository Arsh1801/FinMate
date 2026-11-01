package com.finmate.expense.dto;

import com.finmate.auth.dto.UserSummaryDto;
import com.finmate.category.dto.CategoryDto;
import com.finmate.expense.entity.SharedExpense;
import com.finmate.expense.entity.SplitType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class SharedExpenseDto {
    private Long id;
    private Long groupId;
    private String description;
    private String amount;
    private LocalDate expenseDate;
    private String notes;
    private UserSummaryDto paidBy;
    private CategoryDto category;
    private SplitType splitType;
    private boolean locked;
    private List<ExpenseSplitDto> splits;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer version;

    public SharedExpenseDto() {}

    public static SharedExpenseDto fromEntity(SharedExpense e, boolean locked) {
        SharedExpenseDto dto = new SharedExpenseDto();
        dto.setId(e.getId());
        dto.setGroupId(e.getGroup().getId());
        dto.setDescription(e.getDescription());
        dto.setAmount(e.getAmount() != null ? e.getAmount().toPlainString() : "0.00");
        dto.setExpenseDate(e.getExpenseDate());
        dto.setNotes(e.getNotes());
        dto.setPaidBy(UserSummaryDto.fromEntity(e.getPaidBy()));
        dto.setCategory(e.getCategory() != null ? CategoryDto.fromEntity(e.getCategory()) : null);
        dto.setSplitType(e.getSplitType());
        dto.setLocked(locked);
        if (e.getSplits() != null) {
            dto.setSplits(e.getSplits().stream().map(ExpenseSplitDto::fromEntity).collect(Collectors.toList()));
        }
        dto.setCreatedAt(e.getCreatedAt());
        dto.setUpdatedAt(e.getUpdatedAt());
        dto.setVersion(e.getVersion());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public UserSummaryDto getPaidBy() {
        return paidBy;
    }

    public void setPaidBy(UserSummaryDto paidBy) {
        this.paidBy = paidBy;
    }

    public CategoryDto getCategory() {
        return category;
    }

    public void setCategory(CategoryDto category) {
        this.category = category;
    }

    public SplitType getSplitType() {
        return splitType;
    }

    public void setSplitType(SplitType splitType) {
        this.splitType = splitType;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public List<ExpenseSplitDto> getSplits() {
        return splits;
    }

    public void setSplits(List<ExpenseSplitDto> splits) {
        this.splits = splits;
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
