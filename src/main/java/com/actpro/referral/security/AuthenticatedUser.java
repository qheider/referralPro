package com.actpro.referral.security;

import com.actpro.referral.auth.UserRole;

public record AuthenticatedUser(
        Long userId,
        String username,
        Long companyId,
        UserRole role
) {
}
