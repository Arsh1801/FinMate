package com.finmate.settlement.dto;

import com.finmate.auth.dto.UserSummaryDto;
import com.finmate.settlement.entity.Settlement;
import com.finmate.settlement.entity.SettlementStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class SettlementDto {
    private Long id;
    private Long groupId;
    private UserSummaryDto fromUser;
    private UserSummaryDto toUser;
    private String amount;
    private SettlementStatus status;
    private LocalDate settlementDate;
    private String note;
    private UserSummaryDto recordedBy;
    private LocalDateTime confirmedAt;
    private LocalDateTime createdAt;
    private Integer version;

    public SettlementDto() {}

    public static SettlementDto fromEntity(Settlement s) {
        SettlementDto dto = new SettlementDto();
        dto.setId(s.getId());
        dto.setGroupId(s.getGroup().getId());
        dto.setFromUser(UserSummaryDto.fromEntity(s.getFromUser()));
        dto.setToUser(UserSummaryDto.fromEntity(s.getToUser()));
        dto.setAmount(s.getAmount() != null ? s.getAmount().toPlainString() : "0.00");
        dto.setStatus(s.getStatus());
        dto.setSettlementDate(s.getSettlementDate());
        dto.setNote(s.getNote());
        dto.setRecordedBy(UserSummaryDto.fromEntity(s.getRecordedBy()));
        dto.setConfirmedAt(s.getConfirmedAt());
        dto.setCreatedAt(s.getCreatedAt());
        dto.setVersion(s.getVersion());
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

    public UserSummaryDto getFromUser() {
        return fromUser;
    }

    public void setFromUser(UserSummaryDto fromUser) {
        this.fromUser = fromUser;
    }

    public UserSummaryDto getToUser() {
        return toUser;
    }

    public void setToUser(UserSummaryDto toUser) {
        this.toUser = toUser;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public SettlementStatus getStatus() {
        return status;
    }

    public void setStatus(SettlementStatus status) {
        this.status = status;
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

    public UserSummaryDto getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(UserSummaryDto recordedBy) {
        this.recordedBy = recordedBy;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
