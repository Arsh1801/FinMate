package com.finmate.settlement.dto;

import java.math.BigDecimal;
import java.util.List;

public class GroupBalancesDto {
    private Long groupId;
    private List<MemberBalanceDto> members;
    private String totalExpense;
    private String totalSettled;
    private String netSum;

    public GroupBalancesDto() {}

    public GroupBalancesDto(Long groupId, List<MemberBalanceDto> members, BigDecimal totalExpense, BigDecimal totalSettled, BigDecimal netSum) {
        this.groupId = groupId;
        this.members = members;
        this.totalExpense = totalExpense != null ? totalExpense.toPlainString() : "0.00";
        this.totalSettled = totalSettled != null ? totalSettled.toPlainString() : "0.00";
        this.netSum = netSum != null ? netSum.toPlainString() : "0.00";
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public List<MemberBalanceDto> getMembers() {
        return members;
    }

    public void setMembers(List<MemberBalanceDto> members) {
        this.members = members;
    }

    public String getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(String totalExpense) {
        this.totalExpense = totalExpense;
    }

    public String getTotalSettled() {
        return totalSettled;
    }

    public void setTotalSettled(String totalSettled) {
        this.totalSettled = totalSettled;
    }

    public String getNetSum() {
        return netSum;
    }

    public void setNetSum(String netSum) {
        this.netSum = netSum;
    }
}
