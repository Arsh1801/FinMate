package com.finmate.group.dto;

import com.finmate.group.entity.GroupInvitation;
import com.finmate.group.entity.InvitationStatus;

import java.time.LocalDateTime;

public class InvitationDto {
    private Long id;
    private Long groupId;
    private String groupName;
    private Long invitedUserId;
    private String invitedUserName;
    private String invitedUserEmail;
    private Long invitedById;
    private String invitedByName;
    private InvitationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public InvitationDto() {}

    public static InvitationDto fromEntity(GroupInvitation inv) {
        InvitationDto dto = new InvitationDto();
        dto.setId(inv.getId());
        dto.setGroupId(inv.getGroup().getId());
        dto.setGroupName(inv.getGroup().getName());
        dto.setInvitedUserId(inv.getInvitedUser().getId());
        dto.setInvitedUserName(inv.getInvitedUser().getFullName());
        dto.setInvitedUserEmail(inv.getInvitedUser().getEmail());
        dto.setInvitedById(inv.getInvitedBy().getId());
        dto.setInvitedByName(inv.getInvitedBy().getFullName());
        dto.setStatus(inv.getStatus());
        dto.setCreatedAt(inv.getCreatedAt());
        dto.setExpiresAt(inv.getExpiresAt());
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

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Long getInvitedUserId() {
        return invitedUserId;
    }

    public void setInvitedUserId(Long invitedUserId) {
        this.invitedUserId = invitedUserId;
    }

    public String getInvitedUserName() {
        return invitedUserName;
    }

    public void setInvitedUserName(String invitedUserName) {
        this.invitedUserName = invitedUserName;
    }

    public String getInvitedUserEmail() {
        return invitedUserEmail;
    }

    public void setInvitedUserEmail(String invitedUserEmail) {
        this.invitedUserEmail = invitedUserEmail;
    }

    public Long getInvitedById() {
        return invitedById;
    }

    public void setInvitedById(Long invitedById) {
        this.invitedById = invitedById;
    }

    public String getInvitedByName() {
        return invitedByName;
    }

    public void setInvitedByName(String invitedByName) {
        this.invitedByName = invitedByName;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public void setStatus(InvitationStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
