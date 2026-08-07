package com.actpro.referral.integration.webhook.dto;

/**
 * Business fields parsed out of a verified webhook's raw body. {@code eventId} is the redelivery
 * dedup key; {@code status} feeds {@code ReferralStatusMappingService}; {@code serviceReference}/
 * {@code companyUserReference} are matched against {@code ApiSubmission.companyTransactionReference}/
 * {@code companyCustomerReference} to resolve the target Referral. {@code revenueAmount}/
 * {@code currency} from the source spec's example payload are deliberately not modeled here -
 * they're preserved verbatim in the stored raw payload for Phase 8, which this phase never parses
 * or acts on.
 */
public record IncomingServiceStatusPayload(
        String eventId,
        String eventType,
        String occurredAt,
        String companyUserReference,
        String serviceReference,
        String status
) {
}
