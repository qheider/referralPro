package com.actpro.referral.auth.dto;

public record AcceptInvitationResponse(
        Long userId,
        String username,
        String role
) {
}
