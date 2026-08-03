package com.actpro.referral.ambassador.dto;

import java.time.LocalDateTime;

public record AmbassadorCreationResponse(
        AmbassadorSummaryResponse ambassador,
        String invitationToken,
        LocalDateTime invitationExpiresAt
) {
}
