package com.schoolerp.school_erp.strategy;

public interface NotificationService {
    boolean sendOTP(String recipient, String code);
    boolean sendNotice(String recipient, String message);
}
