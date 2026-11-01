package com.finmate.notification.event;

import com.finmate.transaction.entity.Transaction;

import java.time.LocalDate;

public class TransactionChangedEvent {

    private final Long userId;
    private final Long categoryId;
    private final LocalDate transactionDate;
    private final boolean isExpense;

    public TransactionChangedEvent(Long userId, Long categoryId, LocalDate transactionDate, boolean isExpense) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.transactionDate = transactionDate;
        this.isExpense = isExpense;
    }

    public static TransactionChangedEvent from(Transaction transaction) {
        return new TransactionChangedEvent(
                transaction.getUser().getId(),
                transaction.getCategory() != null ? transaction.getCategory().getId() : null,
                transaction.getTransactionDate(),
                transaction.getType() == com.finmate.transaction.entity.TransactionType.EXPENSE
        );
    }

    public Long getUserId() {
        return userId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public boolean isExpense() {
        return isExpense;
    }
}
