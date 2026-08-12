package com.actpro.referral.revenue.dto;

import com.actpro.referral.campaign.RewardType;
import com.actpro.referral.revenue.AmbassadorRewardStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Used for both admin list and get-by-id responses (unlike ApiSubmission's Summary/Detail split,
 * an AmbassadorReward has no attempt-history sub-resource to justify a separate detail shape).
 */
public record AmbassadorRewardResponse(
        Long id,
        Long campaignId,
        String campaignName,
        Long referralId,
        String referralCode,
        Long ambassadorUserId,
        String ambassadorName,
        RewardType rewardType,
        BigDecimal rewardValue,
        String currency,
        AmbassadorRewardStatus status,
        String holdReason,
        String rejectionReason,
        Long revenueEventId,
        String qualifyingStatus,
        BigDecimal revenueAmount,
        boolean currencyMismatch,
        LocalDateTime createdAt,
        LocalDateTime approvedAt,
        LocalDateTime paidAt,
        LocalDateTime rejectedAt,
        LocalDateTime reversedAt
) {
}
