package com.schoolerp.school_erp.security;

import java.util.Set;
import java.util.UUID;

public class UserContext {
    private static final ThreadLocal<UUID> currentUser = new ThreadLocal<>();
    private static final ThreadLocal<Set<String>> currentRoles = new ThreadLocal<>();

    public static void setCurrentUser(UUID userId) {
        currentUser.set(userId);
    }

    public static UUID getCurrentUser() {
        return currentUser.get();
    }

    public static void setCurrentRoles(Set<String> roles) {
        currentRoles.set(roles);
    }

    public static Set<String> getCurrentRoles() {
        return currentRoles.get();
    }

    public static void clear() {
        currentUser.remove();
        currentRoles.remove();
    }
}
