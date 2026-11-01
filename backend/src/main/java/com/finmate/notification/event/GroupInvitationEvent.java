package com.finmate.notification.event;

import com.finmate.group.entity.GroupInvitation;

public class GroupInvitationEvent {
    private final Long invitationId;
    private final Long groupId;
    private final String groupName;
    private final Long invitedUserId;
    private final String inviterName;

    public GroupInvitationEvent(GroupInvitation invitation) {
        this.invitationId = invitation.getId();
        this.groupId = invitation.getGroup().getId();
        this.groupName = invitation.getGroup().getName();
        this.invitedUserId = invitation.getInvitedUser().getId();
        this.inviterName = invitation.getInvitedBy().getFullName();
    }

    public Long getInvitationId() {
        return invitationId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public Long getInvitedUserId() {
        return invitedUserId;
    }

    public String getInviterName() {
        return inviterName;
    }
}
