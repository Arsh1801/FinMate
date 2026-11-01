package com.finmate.notification.scheduler;

import com.finmate.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationCleanupScheduler.class);

    private final NotificationService notificationService;

    public NotificationCleanupScheduler(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0 2 * * SUN")
    public void cleanupOldNotifications() {
        log.info("Running weekly job to purge notifications older than 90 days (FR-125)...");
        try {
            notificationService.purgeOldNotifications();
            log.info("Notification purge job completed successfully.");
        } catch (Exception e) {
            log.error("Failed to purge old notifications: {}", e.getMessage(), e);
        }
    }
}
