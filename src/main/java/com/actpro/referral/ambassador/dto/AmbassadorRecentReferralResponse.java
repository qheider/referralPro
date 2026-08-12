package com.actpro.referral.ambassador.dto;

import com.actpro.referral.referral.ReferralStatus;

import java.time.LocalDateTime;

public record AmbassadorRecentReferralResponse(
        Long referralId,
        Long campaignId,
        String campaignName,
        String customerName,
        String customerEmail,
        ReferralStatus status,
        LocalDateTime registeredAt,
        LocalDateTime convertedAt
) {
}
