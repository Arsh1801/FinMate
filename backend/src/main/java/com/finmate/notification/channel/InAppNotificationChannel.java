package com.finmate.notification.channel;

import com.finmate.notification.entity.Notification;
import com.finmate.notification.repository.NotificationRepository;
import org.springframework.stereotype.Component;

@Component
public class InAppNotificationChannel implements NotificationChannel {

    private final NotificationRepository notificationRepository;

    public InAppNotificationChannel(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public String getChannelName() {
        return "IN_APP";
    }

    @Override
    public void send(Notification notification) {
        // Notification is persisted in the repository by NotificationService
    }
}
