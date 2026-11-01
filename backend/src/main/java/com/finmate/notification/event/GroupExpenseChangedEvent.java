package com.finmate.notification.event;

import com.finmate.expense.entity.SharedExpense;

import java.util.List;

public class GroupExpenseChangedEvent {
    private final Long expenseId;
    private final Long groupId;
    private final Long paidById;
    private final String description;
    private final String action; // CREATED, UPDATED, DELETED
    private final List<Long> affectedMemberIds;

    public GroupExpenseChangedEvent(SharedExpense expense, String action, List<Long> affectedMemberIds) {
        this.expenseId = expense.getId();
        this.groupId = expense.getGroup().getId();
        this.paidById = expense.getPaidBy().getId();
        this.description = expense.getDescription();
        this.action = action;
        this.affectedMemberIds = affectedMemberIds;
    }

    public Long getExpenseId() {
        return expenseId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public Long getPaidById() {
        return paidById;
    }

    public String getDescription() {
        return description;
    }

    public String getAction() {
        return action;
    }

    public List<Long> getAffectedMemberIds() {
        return affectedMemberIds;
    }
}
