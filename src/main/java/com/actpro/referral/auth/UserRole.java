package com.actpro.referral.auth;

import java.util.Locale;

public enum UserRole {
    PLATFORM_ADMIN,
    COMPANY_ADMIN,
    AMBASSADOR,
    CUSTOMER;

    public static UserRole fromValue(String value) {
        if (value == null || value.isBlank()) {
            return COMPANY_ADMIN;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }

        return UserRole.valueOf(normalized);
    }
}
