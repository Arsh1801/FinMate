package com.finmate.goal.dto;

import com.finmate.goal.entity.GoalContribution;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ContributionDto {
    private Long id;
    private String amount;
    private LocalDate contributionDate;
    private String note;
    private LocalDateTime createdAt;

    public ContributionDto() {}

    public static ContributionDto fromEntity(GoalContribution c) {
        ContributionDto dto = new ContributionDto();
        dto.setId(c.getId());
        dto.setAmount(c.getAmount() != null ? c.getAmount().toPlainString() : "0.00");
        dto.setContributionDate(c.getContributionDate());
        dto.setNote(c.getNote());
        dto.setCreatedAt(c.getCreatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
