package com.actpro.referral.ambassador.dto;

import java.time.LocalDateTime;

public record AmbassadorApplicationApprovalResponse(
        AmbassadorApplicationDetailResponse application,
        AmbassadorSummaryResponse ambassador,
        String invitationToken,
        LocalDateTime invitationExpiresAt
) {
}
