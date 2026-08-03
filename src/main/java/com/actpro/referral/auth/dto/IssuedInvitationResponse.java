package com.actpro.referral.auth.dto;

import java.time.LocalDateTime;

public record IssuedInvitationResponse(
        Long id,
        String token,
        LocalDateTime expiresAt
) {
}
