package com.actpro.referral.company.dto;

import java.time.LocalDateTime;

public record IssuedApiKeyResponse(
        Long id,
        String keyId,
        String apiKey,
        LocalDateTime createdAt
) {
}
