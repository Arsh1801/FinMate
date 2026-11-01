package com.finmate.notification.event;

import com.finmate.settlement.entity.Settlement;
import com.finmate.settlement.entity.SettlementStatus;

public class SettlementStatusChangedEvent {
    private final Long settlementId;
    private final Long groupId;
    private final Long fromUserId;
    private final Long toUserId;
    private final SettlementStatus status;
    private final String amount;

    public SettlementStatusChangedEvent(Settlement s) {
        this.settlementId = s.getId();
        this.groupId = s.getGroup().getId();
        this.fromUserId = s.getFromUser().getId();
        this.toUserId = s.getToUser().getId();
        this.status = s.getStatus();
        this.amount = s.getAmount().toPlainString();
    }

    public Long getSettlementId() {
        return settlementId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public Long getFromUserId() {
        return fromUserId;
    }

    public Long getToUserId() {
        return toUserId;
    }

    public SettlementStatus getStatus() {
        return status;
    }

    public String getAmount() {
        return amount;
    }
}
