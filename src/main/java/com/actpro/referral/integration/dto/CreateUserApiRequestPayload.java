package com.actpro.referral.integration.dto;

/**
 * Outbound wire payload for the company's Create User API. externalRequestId is the idempotency
 * key. firstName/lastName/phone from the source spec's example payload aren't included -
 * ReferralLeadService/PlatformUser only capture a single name + email at lead-capture time today;
 * fixing that is Phase 5 lead-capture-form scope, not this phase's.
 */
public record CreateUserApiRequestPayload(
        String externalRequestId,
        String name,
        String email,
        String campaignCode,
        String referralCode
) {
}
