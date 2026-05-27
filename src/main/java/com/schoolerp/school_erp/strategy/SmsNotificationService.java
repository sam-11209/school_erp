package com.schoolerp.school_erp.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("sms")
public class SmsNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationService.class);

    @Override
    public boolean sendOTP(String recipient, String code) {
        log.info("[SMS Notification] Sending OTP: {} to recipient: {}", code, recipient);
        // Stub for Twilio SMS client
        return true;
    }

    @Override
    public boolean sendNotice(String recipient, String message) {
        log.info("[SMS Notification] Sending Notice: '{}' to recipient: {}", message, recipient);
        return true;
    }
}
