package com.actpro.referral.ambassador.dto;

import com.actpro.referral.ambassador.AmbassadorStatus;

import java.time.LocalDateTime;

/**
 * Response for the public, instant self-service registration endpoint - the account exists but
 * stays unusable until the applicant clicks the onboarding email's accept-invitation link (see
 * AmbassadorApplicationService.registerAmbassador).
 */
public record AmbassadorRegistrationResponse(
        Long ambassadorProfileId,
        AmbassadorStatus status,
        LocalDateTime submittedAt
) {
}
