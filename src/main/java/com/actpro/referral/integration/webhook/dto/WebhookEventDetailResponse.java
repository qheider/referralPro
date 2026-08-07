package com.actpro.referral.integration.webhook.dto;

import com.actpro.referral.integration.webhook.WebhookEventStatus;

import java.time.LocalDateTime;

public record WebhookEventDetailResponse(
        Long id,
        String eventId,
        String eventType,
        WebhookEventStatus status,
        String rawPayload,
        Long matchedReferralId,
        String mappedStatus,
        String failureReason,
        int attempts,
        int maxAttempts,
        LocalDateTime availableAt,
        LocalDateTime createdAt,
        LocalDateTime processedAt
) {
}
