package com.actpro.referral.integration;

public enum ApiSubmissionStatus {
    PENDING,
    PROCESSING,
    SUCCEEDED,
    RETRY_SCHEDULED,
    PERMANENTLY_FAILED,
    // Reserved for a future admin "cancel this stuck submission" action - no endpoint sets this
    // yet, same "wire the field, no consumer yet" precedent as CompanyIntegration in Phase 2.
    CANCELLED
}
