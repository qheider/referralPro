package com.actpro.referral.auth.dto;

import java.time.LocalDateTime;

public record ResendVerificationResponse(
        String email,
        String token,
        LocalDateTime expiresAt
) {
}
