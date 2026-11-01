package com.finmate.notification.listener;

import com.finmate.budget.service.BudgetService;
import com.finmate.notification.event.TransactionChangedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class BudgetAlertListener {

    private final BudgetService budgetService;

    public BudgetAlertListener(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransactionChanged(TransactionChangedEvent event) {
        if (event.isExpense() && event.getTransactionDate() != null) {
            budgetService.evaluateBudgetAlerts(
                    event.getUserId(),
                    event.getTransactionDate().getYear(),
                    event.getTransactionDate().getMonthValue()
            );
        }
    }
}
