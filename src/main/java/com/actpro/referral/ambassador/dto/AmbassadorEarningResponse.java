package com.actpro.referral.ambassador.dto;

import com.actpro.referral.revenue.AmbassadorRewardStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AmbassadorEarningResponse(
        Long id,
        Long campaignId,
        String campaignName,
        String referralCode,
        String rewardType,
        BigDecimal rewardValue,
        String currency,
        AmbassadorRewardStatus status,
        String holdReason,
        LocalDateTime createdAt,
        LocalDateTime approvedAt,
        LocalDateTime paidAt
) {
}
