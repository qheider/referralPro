package com.actpro.referral.auth.dto;

public record VerifyEmailResponse(
        Long userId,
        String username,
        Long companyId
) {
}
