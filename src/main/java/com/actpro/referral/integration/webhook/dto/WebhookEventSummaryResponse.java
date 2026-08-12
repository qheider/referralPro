package com.actpro.referral.integration.webhook.dto;

import com.actpro.referral.integration.webhook.WebhookEventStatus;

import java.time.LocalDateTime;

public record WebhookEventSummaryResponse(
        Long id,
        String eventId,
        String eventType,
        WebhookEventStatus status,
        Long matchedReferralId,
        String mappedStatus,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime processedAt
) {
}
