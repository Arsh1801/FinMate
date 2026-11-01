package com.finmate.group.dto;

import com.finmate.group.entity.GroupMember;
import com.finmate.group.entity.GroupRole;
import com.finmate.group.entity.MemberStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class GroupMemberDto {
    private Long userId;
    private String fullName;
    private String email;
    private GroupRole role;
    private MemberStatus status;
    private String netBalance;
    private LocalDateTime joinedAt;

    public GroupMemberDto() {}

    public GroupMemberDto(Long userId, String fullName, String email, GroupRole role, MemberStatus status, BigDecimal netBalance, LocalDateTime joinedAt) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.status = status;
        this.netBalance = netBalance != null ? netBalance.toPlainString() : "0.00";
        this.joinedAt = joinedAt;
    }

    public static GroupMemberDto fromEntity(GroupMember gm, BigDecimal netBalance) {
        return new GroupMemberDto(
                gm.getUser().getId(),
                gm.getUser().getFullName(),
                gm.getUser().getEmail(),
                gm.getRole(),
                gm.getStatus(),
                netBalance,
                gm.getJoinedAt()
        );
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public GroupRole getRole() {
        return role;
    }

    public void setRole(GroupRole role) {
        this.role = role;
    }

    public MemberStatus getStatus() {
        return status;
    }

    public void setStatus(MemberStatus status) {
        this.status = status;
    }

    public String getNetBalance() {
        return netBalance;
    }

    public void setNetBalance(String netBalance) {
        this.netBalance = netBalance;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }
}
