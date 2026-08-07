package com.actpro.referral.integration.webhook.dto;

import java.time.LocalDateTime;

/** Raw secret shown once, then only stored encrypted - mirrors IssuedApiKeyResponse's intent. */
public record GenerateWebhookSecretResponse(
        String webhookSecret,
        LocalDateTime generatedAt
) {
}
