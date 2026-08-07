package com.actpro.referral.integration.webhook.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payload of the {@code referral.status_changed} outbox event {@code WebhookProcessingService}
 * publishes whenever it actually applies a status transition. Consumed by
 * {@code revenue.RevenueEventOutboxEventHandler}, which re-reads {@code Referral.status} fresh
 * rather than trusting a previous/new status carried here (see the publish call site's Javadoc) -
 * {@code revenueAmount}/{@code currency} are carried here because they have no other persisted
 * home on {@link com.actpro.referral.referral.Referral}.
 */
public record ReferralStatusChangedEventPayload(
        Long referralId,
        BigDecimal revenueAmount,
        String currency,
        LocalDateTime occurredAt
) {
}
