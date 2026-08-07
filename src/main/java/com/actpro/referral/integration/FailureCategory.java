package com.actpro.referral.integration;

/**
 * Classifies a failed {@link CreateUserApiClient} call so {@link ApiSubmissionDispatchService}
 * knows whether to retry. TIMEOUT/CONNECTION_ERROR/RATE_LIMITED/SERVER_ERROR are transient
 * (retried with backoff); CLIENT_ERROR/AUTH_ERROR are permanent (no retry). AUTH_ERROR
 * additionally flips the owning CompanyIntegration.status to ERROR since it signals bad/expired
 * credentials the admin needs to fix, not a transient downstream issue.
 */
public enum FailureCategory {
    NONE,
    TIMEOUT,
    CONNECTION_ERROR,
    RATE_LIMITED,
    SERVER_ERROR,
    CLIENT_ERROR,
    AUTH_ERROR
}
