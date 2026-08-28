package com.actpro.referral.ambassador.dto;

import com.actpro.referral.referral.ReferralLinkStatus;

import java.time.LocalDateTime;

public record ReferralLinkSummaryResponse(
        Long id,
        String publicToken,
        String referralUrl,
        String qrCodeUrl,
        String destinationUrl,
        ReferralLinkStatus status,
        Long clickCount,
        LocalDateTime expiresAt
) {
}
