package com.actpro.referral.integration.webhook.dto;

import java.math.BigDecimal;

/**
 * Business fields parsed out of a verified webhook's raw body. {@code eventId} is the redelivery
 * dedup key; {@code status} feeds {@code ReferralStatusMappingService}; {@code serviceReference}/
 * {@code companyUserReference} are matched against {@code ApiSubmission.companyTransactionReference}/
 * {@code companyCustomerReference} to resolve the target Referral. {@code revenueAmount}/
 * {@code currency} are informational, consumed only by Phase 8's {@code revenue.RevenueEventService}
 * (via the {@code referral.status_changed} outbox event {@code WebhookProcessingService} publishes) -
 * they never feed reward payout math, which always comes from the campaign's snapshotted reward
 * rule instead.
 */
public record IncomingServiceStatusPayload(
        String eventId,
        String eventType,
        String occurredAt,
        String companyUserReference,
        String serviceReference,
        String status,
        BigDecimal revenueAmount,
        String currency
) {
}
