package com.finmate.notification.listener;

import com.finmate.common.util.MoneyUtils;
import com.finmate.notification.entity.NotificationType;
import com.finmate.notification.event.GroupExpenseChangedEvent;
import com.finmate.notification.event.GroupInvitationEvent;
import com.finmate.notification.event.SettlementStatusChangedEvent;
import com.finmate.notification.service.NotificationService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;

@Component
public class NotificationListener {

    private final NotificationService notificationService;

    public NotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGroupExpenseChanged(GroupExpenseChangedEvent event) {
        String title = "Group Expense " + event.getAction();
        String message = String.format("A shared expense '%s' was %s in your group.",
                event.getDescription(), event.getAction().toLowerCase());

        for (Long memberId : event.getAffectedMemberIds()) {
            if (!memberId.equals(event.getPaidById())) {
                notificationService.createNotification(
                        memberId,
                        NotificationType.GROUP_EXPENSE,
                        title,
                        message,
                        "GROUP",
                        event.getGroupId(),
                        "group_exp_" + event.getExpenseId() + "_" + event.getAction() + "_" + memberId
                );
            }
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSettlementStatusChanged(SettlementStatusChangedEvent event) {
        BigDecimal amt = new BigDecimal(event.getAmount());
        String formattedAmt = MoneyUtils.formatIndianCurrency(amt);

        switch (event.getStatus()) {
            case PENDING -> {
                notificationService.createNotification(
                        event.getToUserId(),
                        NotificationType.SETTLEMENT_PENDING,
                        "Settlement Awaiting Confirmation",
                        "A payment of " + formattedAmt + " has been recorded and is waiting for your confirmation.",
                        "SETTLEMENT",
                        event.getSettlementId(),
                        "settlement_pending_" + event.getSettlementId()
                );
            }
            case CONFIRMED -> {
                notificationService.createNotification(
                        event.getFromUserId(),
                        NotificationType.SETTLEMENT_RESULT,
                        "Settlement Confirmed",
                        "Your payment of " + formattedAmt + " was confirmed.",
                        "SETTLEMENT",
                        event.getSettlementId(),
                        "settlement_confirmed_" + event.getSettlementId()
                );
            }
            case REJECTED -> {
                notificationService.createNotification(
                        event.getFromUserId(),
                        NotificationType.SETTLEMENT_RESULT,
                        "Settlement Rejected",
                        "Your recorded payment of " + formattedAmt + " was rejected.",
                        "SETTLEMENT",
                        event.getSettlementId(),
                        "settlement_rejected_" + event.getSettlementId()
                );
            }
            case REVERSED -> {
                notificationService.createNotification(
                        event.getFromUserId(),
                        NotificationType.SETTLEMENT_RESULT,
                        "Settlement Reversed",
                        "The confirmed settlement of " + formattedAmt + " was reversed by the receiver.",
                        "SETTLEMENT",
                        event.getSettlementId(),
                        "settlement_reversed_" + event.getSettlementId()
                );
            }
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGroupInvitation(GroupInvitationEvent event) {
        notificationService.createNotification(
                event.getInvitedUserId(),
                NotificationType.GROUP_INVITATION,
                "Group Invitation",
                event.getInviterName() + " invited you to join '" + event.getGroupName() + "'.",
                "INVITATION",
                event.getInvitationId(),
                "group_invitation_" + event.getInvitationId()
        );
    }
}
