package com.finmate.user.dto;

public class AdminStatsDto {
    private long totalUsers;
    private long activeUsers;
    private long registrationsLast30Days;
    private long totalGroups;
    private long totalTransactions;

    public AdminStatsDto() {}

    public AdminStatsDto(long totalUsers, long activeUsers, long registrationsLast30Days, long totalGroups, long totalTransactions) {
        this.totalUsers = totalUsers;
        this.activeUsers = activeUsers;
        this.registrationsLast30Days = registrationsLast30Days;
        this.totalGroups = totalGroups;
        this.totalTransactions = totalTransactions;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(long activeUsers) {
        this.activeUsers = activeUsers;
    }

    public long getRegistrationsLast30Days() {
        return registrationsLast30Days;
    }

    public void setRegistrationsLast30Days(long registrationsLast30Days) {
        this.registrationsLast30Days = registrationsLast30Days;
    }

    public long getTotalGroups() {
        return totalGroups;
    }

    public void setTotalGroups(long totalGroups) {
        this.totalGroups = totalGroups;
    }

    public long getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(long totalTransactions) {
        this.totalTransactions = totalTransactions;
    }
}
