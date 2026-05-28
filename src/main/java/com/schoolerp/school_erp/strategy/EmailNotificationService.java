package com.schoolerp.school_erp.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("email")
public class EmailNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    @Override
    public boolean sendOTP(String recipient, String code) {
        log.info("[Email Notification] Sending OTP: {} to email: {}", code, recipient);
        return true;
    }

    @Override
    public boolean sendNotice(String recipient, String message) {
        log.info("[Email Notification] Sending Notice: '{}' to email: {}", message, recipient);
        return true;
    }
}
