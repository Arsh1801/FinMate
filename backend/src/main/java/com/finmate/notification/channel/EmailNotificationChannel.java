package com.finmate.notification.channel;

import com.finmate.notification.entity.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class EmailNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationChannel.class);

    private final Optional<JavaMailSender> mailSender;

    public EmailNotificationChannel(Optional<JavaMailSender> mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public String getChannelName() {
        return "EMAIL";
    }

    @Override
    @Async
    public void send(Notification notification) {
        if (!Boolean.TRUE.equals(notification.getUser().getEmailNotifications())) {
            return;
        }

        String recipientEmail = notification.getUser().getEmail();
        log.info("[EMAIL DISPATCH] To: {} | Subject: {} | Message: {}",
                recipientEmail, notification.getTitle(), notification.getMessage());

        mailSender.ifPresent(sender -> {
            try {
                SimpleMailMessage mailMessage = new SimpleMailMessage();
                mailMessage.setTo(recipientEmail);
                mailMessage.setSubject("FinMate: " + notification.getTitle());
                mailMessage.setText(notification.getMessage());
                sender.send(mailMessage);
            } catch (Exception e) {
                log.warn("Initial email send failed for {}: {}. Retrying once...", recipientEmail, e.getMessage());
                try {
                    SimpleMailMessage retryMsg = new SimpleMailMessage();
                    retryMsg.setTo(recipientEmail);
                    retryMsg.setSubject("FinMate: " + notification.getTitle());
                    retryMsg.setText(notification.getMessage());
                    sender.send(retryMsg);
                } catch (Exception retryEx) {
                    log.error("Email send failed after retry for {}: {}", recipientEmail, retryEx.getMessage());
                }
            }
        });
    }
}
