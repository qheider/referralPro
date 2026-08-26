package com.actpro.referral.auth.dto;

public record ResetPasswordResponse(
        Long userId,
        String username
) {
}
