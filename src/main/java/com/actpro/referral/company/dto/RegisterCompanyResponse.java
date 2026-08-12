package com.actpro.referral.company.dto;

import java.time.LocalDateTime;

public record RegisterCompanyResponse(
        Long companyId,
        String name,
        String apiKey,
        String adminEmail,
        // Returned directly because there is no email delivery worker yet (see Phase 13 of
        // phases_tracker.txt) - once one exists, this should be emailed instead of returned here.
        String emailVerificationToken,
        LocalDateTime emailVerificationTokenExpiresAt
) {
}
