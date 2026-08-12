package com.actpro.referral.security;

import com.actpro.referral.auth.UserRole;

/**
 * Single-shot snapshot of the authenticated principal for the current request, formalizing what
 * {@link CurrentUserService}'s individual getters already resolve from {@link AuthenticatedUser}.
 * Intended as the actor stamp for cross-cutting concerns that need "who did this" as one value
 * (e.g. outbox events, audit trail) rather than a company/ambassador access check.
 */
public record CurrentActor(Long userId, String username, Long companyId, UserRole role) {

    public boolean hasRole(UserRole expected) {
        return role == expected;
    }
}
