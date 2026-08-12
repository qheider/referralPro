package com.actpro.referral.integration;

/**
 * Auth mechanisms {@link CreateUserApiClient} can build a request with. OAuth2 client-credentials
 * (token fetch/cache/refresh) is deliberately not supported yet - a documented follow-up, not
 * built in Phase 6, since no concrete target API currently requires it.
 */
public enum IntegrationAuthType {
    NONE,
    API_KEY,
    BEARER_TOKEN,
    BASIC
}
