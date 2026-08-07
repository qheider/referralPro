package com.actpro.referral.integration.webhook;

public enum WebhookEventStatus {
    RECEIVED,
    PROCESSING,
    PROCESSED,
    // Mapped status resolved but the transition wasn't applied (backward, or the referral is
    // already in a terminal state) - not an error, logged for audit via mappedStatus.
    IGNORED,
    RETRY_SCHEDULED,
    // Unmatched reference or unmapped status string (a data/config problem, not retried), or a
    // transient failure that exhausted its retry budget.
    MANUAL_REVIEW
}
