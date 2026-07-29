package com.actpro.referral.ambassador.dto;

import com.actpro.referral.referral.ReferralStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AmbassadorReferralResponse(
        Long referralId,
        Long campaignId,
        String campaignName,
        String referralCode,
        String customerName,
        String customerEmail,
        ReferralStatus status,
        LocalDateTime createdAt,
        LocalDateTime registeredAt,
        LocalDateTime convertedAt,
        String bookingId,
        String rentalId,
        BigDecimal discountAmount,
        String currency
) {
}
