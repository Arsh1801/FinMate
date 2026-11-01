package com.finmate.dashboard.dto;

public class GroupSummaryDto {
    private String youAreOwed;
    private String youOwe;
    private String netBalance;
    private int activeGroupsCount;

    public GroupSummaryDto() {}

    public GroupSummaryDto(String youAreOwed, String youOwe, String netBalance, int activeGroupsCount) {
        this.youAreOwed = youAreOwed;
        this.youOwe = youOwe;
        this.netBalance = netBalance;
        this.activeGroupsCount = activeGroupsCount;
    }

    public String getYouAreOwed() {
        return youAreOwed;
    }

    public void setYouAreOwed(String youAreOwed) {
        this.youAreOwed = youAreOwed;
    }

    public String getYouOwe() {
        return youOwe;
    }

    public void setYouOwe(String youOwe) {
        this.youOwe = youOwe;
    }

    public String getNetBalance() {
        return netBalance;
    }

    public void setNetBalance(String netBalance) {
        this.netBalance = netBalance;
    }

    public int getActiveGroupsCount() {
        return activeGroupsCount;
    }

    public void setActiveGroupsCount(int activeGroupsCount) {
        this.activeGroupsCount = activeGroupsCount;
    }
}
