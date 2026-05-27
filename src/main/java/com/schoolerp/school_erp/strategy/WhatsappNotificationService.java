package com.schoolerp.school_erp.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component("whatsapp")
public class WhatsappNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(WhatsappNotificationService.class);

    @Override
    public boolean sendOTP(String recipient, String code) {
        String message = "Your OTP code is: " + code;
        String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
        String redirectUrl = "https://wa.me/" + recipient + "?text=" + encodedMessage;
        log.info("[WhatsApp Notification] Sending OTP: {} to recipient: {}", code, recipient);
        log.info("[WhatsApp Notification] Simulating WhatsApp redirect URL: {}", redirectUrl);
        return true;
    }

    @Override
    public boolean sendNotice(String recipient, String message) {
        String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
        String redirectUrl = "https://wa.me/" + recipient + "?text=" + encodedMessage;
        log.info("[WhatsApp Notification] Sending Notice: '{}' to recipient: {}", message, recipient);
        log.info("[WhatsApp Notification] Simulating WhatsApp redirect URL: {}", redirectUrl);
        return true;
    }
}
