package com.schoolerp.school_erp.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NotificationFactory {

    private final Map<String, NotificationService> services;

    @Autowired
    public NotificationFactory(Map<String, NotificationService> services) {
        this.services = services;
    }

    public NotificationService getService(String type) {
        NotificationService service = services.get(type.toLowerCase());
        if (service == null) {
            throw new IllegalArgumentException("Unknown notification service type: " + type);
        }
        return service;
    }
}
