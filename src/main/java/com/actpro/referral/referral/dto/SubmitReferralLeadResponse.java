package com.actpro.referral.referral.dto;

import com.actpro.referral.referral.ReferralStatus;

import java.time.LocalDateTime;

public record SubmitReferralLeadResponse(
        String referralCode,
        ReferralStatus status,
        LocalDateTime registeredAt
) {
}
