package com.actpro.referral.company.dto;

import com.actpro.referral.company.CompanyApiKeyStatus;

import java.time.LocalDateTime;

public record ApiKeySummaryResponse(
        Long id,
        String keyId,
        String secretPreview,
        CompanyApiKeyStatus status,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        LocalDateTime lastUsedAt,
        LocalDateTime revokedAt
) {
}
