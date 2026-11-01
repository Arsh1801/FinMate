package com.finmate.notification.channel;

import com.finmate.notification.entity.Notification;

public interface NotificationChannel {

    String getChannelName();

    void send(Notification notification);
}
